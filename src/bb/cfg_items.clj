(ns cfg-items
  "`cfg-items` stands for configuration items."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [dag]
            [dag.map]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            [utils]))

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
   [:install-options
    {:optional true,
     :description "What options to be added when installing the formula."}
    [:vector :string]]
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
   [:deps
    {:optional true,
     :description "List of aliases that this configuration item depends on."}
    [:sequential :keyword]]
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
  [{:keys [tap formula package-manager cask install-options], :as _cfg-item} os]
  (when (= package-manager :brew)
    {:cfg-version-cmds [(vec (concat ["brew" "list"]
                                     (when (and (= os :macos)
                                                cask) ["--cask"])
                                     [formula "--versions"]))],
     :check-cmds [], ;; brew cfg-item is checking all managed cfg-items at
     ;; once.
     :clean-cmds [["brew" "cleanup" formula]],
     :cfg-item-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds (->> [(vec (concat ["brew" "reinstall"]
                                      (when (and (= os :macos)
                                                 cask) ["--cask"])
                                      [formula "-q"]
                                      (when install-options install-options)))]
                        (concat (when tap [["brew" "tap" tap]]))
                        vec),
     :update-cmds [["brew" "upgrade" formula]]}))

(defn npm-update
  "Create commands for an npm package manager."
  [{:keys [npm-deps package-manager], :as _cfg-item} _os]
  (when (= package-manager :npm)
    {:cfg-version-cmds [],
     :check-cmds (mapv (fn [npm-dep] ["npm" "doctor" npm-dep]) npm-deps),
     :clean-cmds [], ;; npm cache clean is discouraged by npm.
     :cfg-item-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds (mapv (fn [npm-dep] ["npm" "install" "-g" npm-dep])
                         npm-deps),
     :update-cmds (mapv (fn [npm-dep] ["npm" "update" "-g" npm-dep])
                        npm-deps)}))

(defn manual-update
  "Create commands for the manual package manager."
  [{:keys [package-manager], :as cfg-item} _os]
  (when (= package-manager :manual)
    (select-keys cfg-item
                 [:cfg-version-cmds :check-cmds :clean-cmds :init-cmds
                  :install-cmds :update-cmds])))

(defn common-update
  "Create common commands for the package manager."
  [{:keys [tmp-files clean-cmds pre-reqs deps tmp-dirs post-package cfg-files],
    :as _cfg-item} _os]
  (cond-> {}
    (seq clean-cmds) (assoc :clean-cmds (vec clean-cmds))
    (seq tmp-files) (update :clean-cmds
                            concat
                            (->> tmp-files
                                 (mapv (fn [tmp-file] ["rm" "-f" tmp-file]))))
    (seq tmp-dirs) (update :clean-cmds
                           (comp vec concat)
                           (->> tmp-dirs
                                (mapv (fn [tmp-dir] ["rm" "-fr" tmp-dir]))))
    cfg-files (assoc :cfg-files (vec cfg-files))
    post-package (assoc :post-package post-package)
    pre-reqs (update :cfg-item-deps (comp vec concat) (keys pre-reqs))
    deps (update :cfg-item-deps (comp vec dedupe sort vec concat) deps)))

(defn expand-package-managers
  [cfg-items os]
  (->> cfg-items
       (mapv (fn [[cfg-item-name cfg-item]]
               [cfg-item-name
                (->> ((juxt brew-update npm-update manual-update common-update)
                      cfg-item os)
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
                                                  {:deps deps-name})))]
                   (concat [[cfg-item new-deps-name]]
                           (:pre-reqs cfg-item-val)))))
       (into {})))

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
  (cond-> (read-data-as-resource cfg-filename)
    (some? os) (utils/deep-merge (->> os
                                      cfg-envs
                                      (format "%s/%s.edn" cfg-dir)
                                      read-data-as-resource))))

(defn limit-configurations
  [configurations cfg-items]
  (cond-> configurations (seq cfg-items) (select-keys cfg-items)))

(defn cfg-items-by-layers
  [cfg-items]
  (dag/topological-layers cfg-items (dag.map/simple :cfg-item-deps) 10))

(defn cfg-items-sorted
  [cfg-items]
  (->> (dag/topological-layers cfg-items (dag.map/simple :cfg-item-deps) 10)
       #_(dag/ordered-nodes cfg-items)))

(defn prepare
  "Build a configuration items for `os` and limited to the names in `cfg-item-names`.
  If `cfg-item-names` is empty, all elements are returned."
  [cfg-item-names os]
  (-> (cond-> (cfg-items/read-configurations os) (seq cfg-item-names)
              (cfg-items/limit-configurations cfg-item-names))
      cfg-items/develop-pre-reqs
      (cfg-items/expand-package-managers os)))

(defn ordered-cfg-items
  [cfg-items cfg-items-by-layers]
  (->> cfg-items-by-layers
       :sorted
       (apply concat)
       (mapcat #(vector % (get cfg-items %)))
       (apply array-map)))
