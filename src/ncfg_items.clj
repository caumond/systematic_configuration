(ns ncfg-items
  "`cfg-items` stands for `configuration-items`, which is a sequence of item like this:

  ```clojure
  :clever{:cfg-files [\"~/.config/clever-cloud/clever-tools.json\"],
          :formula \"clever-tools\"}
  ```"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [deps-graph]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            [utils]
            [deps-graph.map :as graph-map]))

(def malli-registry (merge (m/default-schemas) (mu/schemas)))

(def cmd [:vector :string])

(def cmds [:sequential cmd])

(def cfg-item-name :keyword)

(def brew-package-manager
  "Package management with brew."
  [:map {:closed true}
   [:package-manager {:description "one package manager among existings."}
    [:enum :brew]]
   [:formula {:description "`brew` formula to install the `cfg-item`."} :string]
   [:tap {:optional true, :description "`brew` tap where to find the formula."}
    :string]])

(def npm-package-manager
  "Package management with npm."
  [:map {:closed true} [:package-manager [:enum :npm]]
   [:npm-dep {:description "`npm` dependency."} :string]])

(def manual-package-manager
  "No package management, done manually with sh commands."
  [:map {:closed true} [:package-manager [:enum :manual]]
   [:check-cmd
    {:optional true, :description "Check the installation of the `cfg-item`."}
    cmd]
   [:clean-cmds {:optional true, :description "Clean the `cfg-item`."} cmds]
   [:init-cmds
    {:optional true,
     :description "Commands to init - run once - the configuration item."} cmds]
   [:install-cmds
    {:description "Commands describing how to manually install the `cfg-item`."}
    cmds]
   [:update-cmds
    {:optional true, :description "Update the installation of `cfg-item`."}
    cmds]
   [:cfg-version-cmd
    {:optional true, :description "Command for displaying the version."} cmd]])

(def cfg-item-schema
  "One `cfg-item` schema, is one of the package-manager and some global properites available for all of them."
  [:schema
   {:registry
    {::app
     [:union
      [:or brew-package-manager npm-package-manager manual-package-manager]
      [:map {:closed false}
       [:description
        {:optional true, :description "Optional description of the `cfg-item`."}
        :string]
       [:post-package
        {:optional true,
         :description "Command to setup after package has been installed."}
        cmds]
       [:pre-reqs
        {:optional true,
         :description
         "List of cfg-item pre requisites. They have the exact same compatibility."}
        [:map-of cfg-item-name [:ref ::app]]]
       [:cfg-files
        {:optional true,
         :description "Configuration files of this `cfg-item` to save."}
        [:vector :string]]
       [:tmp-files {:optional true, :description "Temporary files to remove."}
        [:vector :string]]
       [:tmp-dirs
        {:optional true, :description "Temporary directory to remove."}
        [:vector :string]]]]}} ::app])

(def cfg-items-schema
  (m/schema [:map-of cfg-item-name cfg-item-schema] {:registry malli-registry}))

(def ^:private cfg-dir "Directory where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn brew-update
  "Create commands for a brew package manager."
  [{:keys [tap formula package-manager], :as _cfg-item}]
  (when (= package-manager :brew)
    {:cfg-version-cmd ["brew" "list" formula "--versions"],
     :check-cmd [], ;; brew cfg-item is checking all managed cfg-items at
                    ;; once.
     :clean-cmds [["brew" "cleanup" formula]],
     :init-cmds [], ;; no need
     :install-cmds (->> [["brew" "install" formula "-q"]]
                        (concat (when tap [["brew" "tap" tap]]))
                        vec),
     :package-manager package-manager,
     :update-cmds [["brew" "upgrade" formula]],
     :graph-deps [package-manager]}))

(defn npm-update
  "Create commands for an npm package manager."
  [{:keys [npm-dep package-manager], :as _cfg-item}]
  (when (= package-manager :npm)
    {:check-cmd [], ;; Don't know how to do check with npm
     :clean-cmds [], ;; Don't know how to do clean with npm
     :install-cmds [["npm" "install" "-g" npm-dep]],
     :init-cmds [], ;; no need
     :update-cmds [["npm" "update" "-g" npm-dep]],
     :cfg-version-cmd ["npm" "version" "-g"],
     :package-manager package-manager,
     :graph-deps [package-manager]}))

(defn manual-update
  "Create commands for the manual package manager."
  [{:keys [package-manager], :as cfg-item}]
  (when (= package-manager :manual) cfg-item))

(defn common-update
  "Create common commands for the package manager."
  [{:keys [tmp-files tmp-dirs post-package cfg-files], :as _cfg-item}]
  (cond-> {}
    tmp-files (assoc :clean-cmds
                     (->> tmp-files
                          (mapv (fn [tmp-file] ["rm" "-f" tmp-file]))))
    cfg-files (assoc :cfg-files cfg-files)
    post-package (assoc :post-package post-package)
    tmp-dirs (update :clean-cmds
                     conj
                     (->> tmp-dirs
                          (mapv (fn [tmp-dir] ["rm" "-fr" tmp-dir]))))))

(defn expand
  [cfg-items]
  (->> cfg-items
       (mapv (fn [[cfg-item-name cfg-item]]
               [cfg-item-name
                (->> cfg-item
                     ((juxt brew-update npm-update manual-update common-update))
                     (apply merge))]))
       (into {})))

(defn- read-data-as-resource
  [filename]
  (try (->> filename
            io/resource
            slurp
            edn/read-string)
       (catch Exception _
         (println (format "File `%s` could not be loaded" filename))
         nil)))

(defn- develop-pre-req-1
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[cfg-item cfg-item-val]]
                 (let [deps-name (vec (keys (:pre-reqs cfg-item-val)))
                       new-deps-name (-> cfg-item-val
                                         (dissoc :pre-reqs)
                                         (merge (when-not (empty? deps-name)
                                                  {::graph-deps deps-name})))]
                   (concat [[cfg-item new-deps-name]]
                           (:pre-reqs cfg-item-val)))))
       (into {})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn develop-pre-reqs
  [cfg-items]
  (loop [cfg-items cfg-items
         max-loops 10]
    (let [updated-cfg-items (develop-pre-req-1 cfg-items)]
      (if (= updated-cfg-items cfg-items)
        cfg-items
        (if (pos? max-loops)
          (recur updated-cfg-items (dec max-loops))
          updated-cfg-items)))))

(defn- validate-cfg
  [file-content]
  (when-not (m/validate cfg-items-schema file-content)
    (println "Error in the configuration: ")
    (println (->> file-content
                  (m/explain cfg-items-schema)
                  me/humanize)))
  file-content)

(defn read-configurations
  [os]
  (-> (read-data-as-resource "ncfg_item.edn")
      validate-cfg
      (utils/deep-merge (->> os
                             cfg-envs
                             (format "%s/%s.edn" cfg-dir)
                             read-data-as-resource))))

(defn limit-configurations
  [configurations cfg-items]
  (cond-> configurations (seq cfg-items) (select-keys cfg-items)))

(defn cfg-items-sorted
  [cfg-items]
  (deps-graph/topological-layers cfg-items deps-graph.map/simple 10))
