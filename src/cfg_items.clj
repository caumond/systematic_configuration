(ns cfg-items
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
   [:cask {:description "Is the formula a cask?", :optional true} :boolean]
   [:formula {:description "`brew` formula to install the `cfg-item`."} :string]
   [:install-options {:optional true :description "What options to be added when installing the formula."} :string]
   [:tap {:optional true, :description "`brew` tap where to find the formula."}
    :string]])

(def npm-package-manager
  "Package management with npm."
  [:map {:closed true} [:package-manager [:enum :npm]]
   [:npm-deps {:description "`npm` dependency."} [:vector :string]]])

(def manual-package-manager
  "No package management, done manually with sh commands."
  [:map {:closed true} [:package-manager [:enum :manual]]
   [:check-cmds
    {:optional true, :description "Check the installation of the `cfg-item`."}
    cmds]
   [:clean-cmds {:optional true, :description "Clean the `cfg-item`."} cmds]
   [:init-cmds
    {:optional true,
     :description "Commands to init - run once - the configuration item."} cmds]
   [:install-cmds
    {:description "Commands describing how to manually install the `cfg-item`.",
     :optional true} cmds]
   [:update-cmds
    {:optional true, :description "Update the installation of `cfg-item`."}
    cmds]
   [:cfg-version-cmds
    {:optional true, :description "Command for displaying the version."} cmds]])

(def common-behavior
  [:map
   [:description
    {:optional true, :description "Optional description of the `cfg-item`."}
    :string]
   [:post-package
    {:optional true,
     :description "Command to setup after package has been installed."} cmds]
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
   [:tmp-dirs {:optional true, :description "Temporary directory to remove."}
    [:vector :string]]])

(def assembly
  [:or [:union npm-package-manager common-behavior]
   [:union manual-package-manager common-behavior]
   [:union brew-package-manager common-behavior]])

(def registry (assoc malli-registry ::app assembly))

(def cfg-items-schema [:map-of cfg-item-name [:ref ::app]])

(def ^:private cfg-dir "Directory where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn brew-update
  "Create commands for a brew package manager."
  [{:keys [tap formula package-manager cask install-options], :as _cfg-item}]
  (when (= package-manager :brew)
    {:cfg-version-cmds [(vec (concat ["brew" "list"]
                                     (when cask ["--cask"])
                                     [formula "--versions"]))],
     :check-cmds [], ;; brew cfg-item is checking all managed cfg-items at
     ;; once.
     :clean-cmds [["brew" "cleanup" formula]],
     :graph-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds
     (->> [(vec (concat ["brew" "install"] (when cask ["--cask"]) [formula "-q"] [install-options]))]
          (concat (when tap [["brew" "tap" tap]]))
          vec),
     :package-manager package-manager,
     :update-cmds [["brew" "upgrade" formula]]}))

(defn npm-update
  "Create commands for an npm package manager."
  [{:keys [npm-deps package-manager], :as _cfg-item}]
  (when (= package-manager :npm)
    {:cfg-version-cmds [],
     :check-cmds (mapv (fn [npm-dep] ["npm" "doctor" npm-dep]) npm-deps),
     :clean-cmds [], ;; npm cache clean is discouraged by npm.
     :graph-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds (mapv (fn [npm-dep] ["npm" "install" "-g" npm-dep])
                         npm-deps),
     :package-manager package-manager,
     :update-cmds (mapv (fn [npm-dep] ["npm" "update" "-g" npm-dep])
                        npm-deps)}))

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
                     (comp vec concat)
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

(defn read-data-as-resource
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

(defn validate-cfg
  [file-content]
  (when-not (m/validate cfg-items-schema file-content {:registry registry})
    {:error (->> file-content
                 (m/explain (m/schema cfg-items-schema {:registry registry}))
                 me/with-spell-checking
                 me/humanize)}))

(def cfg-filename "cfg_item.edn")

(defn read-configurations
  [os]
  (-> (read-data-as-resource cfg-filename)
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

;; (defn validate-data
;;   "Return true if the data is matching the schema
;;   Params:
;;   * `schema` schema to match
;;   * `data` data to check appliance to schema"
;;   [schema data]
;;   (-> schema
;;       (malli/schema {:registry registry})
;;       (malli/validate data)))

;; (defn validate-data-humanize
;;   "Returns nil if valid, the error message otherwise.

;;   Params:
;;   * `schema` schema to match
;;   * `data` data to check appliance to schema"
;;   [schema data]
;;   (when-not (-> schema
;;                 (malli/schema {:registry registry})
;;                 (validate-data data))
;;     {:error (-> (malli/explain schema data)
;;                 malli-error/with-spell-checking
;;                 malli-error/humanize)
;;      :schema schema
;;      :data data}))
