(ns cfg-items
  "Configuration items management

  Design decision:
  * The dependencies are stored as item subkeys
    * Rationale: it is more easy to read, and removing the item should remove its dependency. If the dependency is needed as a pure item, it can be added in the root already"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [deps-graph]
            [malli.core :as m]
            [malli.error :as me]
            [utils]))

(def cmd [:vector :string])

(def cmds [:vector cmd])

(def app-schema
  [:schema
   {:registry
    {::app [:map {:closed true} [:cfgs {:optional true} [:vector :string]]
            [:check {:optional true} cmds] [:clean {:optional true} cmds]
            [:description {:optional true} :string]
            [:formula {:optional true} :string]
            [:install {:optional true} cmds]
            [:init {:optional true} cmds]
            [:node-deps {:optional true} :string]
            [:package {:optional true} :string]
            [:pre-reqs {:optional true} [:map-of :keyword [:ref ::app]]]
            [:tap {:optional true} :string]
            [:tmp-dirs {:optional true} [:vector :string]]
            [:update {:optional true} cmds] [:version {:optional true} cmd]]}}
   ::app])

(def apps-schema [:map-of :keyword app-schema])

(def ^:private cfg-dir "Where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn- assoc-concat
  "Add the collection `coll` to the value of the keyword `kw` in the map `val`"
  [val kw coll]
  (update val kw #(vec (concat coll %))))

(defn- pip-update-cfg-item
  "If the parameter is a configuration with a `package`, then creates all subsequent keys"
  [{:keys [package], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? package)
           (-> {}
               (assoc-concat ::graph-deps [:pip])
               (assoc-concat :install [["pip3" "install" package]])
               (assoc-concat :update [["pip3" "install" "--upgrade" package]])
               (assoc-concat :check [["pip3" "check" package]])))))

(defn- brew-update-cfg-item
  [{:keys [tap formula], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? formula)
           (-> {}
               (assoc-concat ::graph-deps [:brew])
               (assoc-concat :install
                             (concat (when tap [["brew" "tap" tap]])
                                     [["brew" "install" formula]]))
               (assoc-concat :update [["brew" "upgrade" formula]])
               (assoc-concat :version ["brew" "list" formula "--versions"])))))

(defn- npm-cfg-item
  [{:keys [npm-deps], :as cfg-item-val}]
  (merge cfg-item-val
         (when npm-deps {:install [["npm" "install" "-g" npm-deps]]})))

(defn- tmp-dirs-cfg-item
  [{:keys [tmp-dirs], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? tmp-dirs)
           {:clean (mapv (fn [clean-dir] ["rm" "-fr" clean-dir]) tmp-dirs)})))

(defn- tmp-files-cfg-item
  [{:keys [tmp-files], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? tmp-files)
           {:clean (mapv (fn [tmp-file] ["rm" "-f" tmp-file]) tmp-files)})))

(defn- expand-pre-built
  "For each predefined type"
  [configurations]
  (->> configurations
       (mapv (fn [[cfg-item val]]
               [cfg-item
                (-> val
                    brew-update-cfg-item
                    pip-update-cfg-item
                    tmp-dirs-cfg-item
                    npm-cfg-item
                    tmp-files-cfg-item)]))
       (into {})))

(defn- read-data-as-resource
  "Read the filename in the classpath as a resource
  Params:
  * `filename` name of the path to load relatively to a classpath"
  [filename]
  (try (->> filename
            io/resource
            slurp
            edn/read-string)
       (catch Exception _
         (println (format "File `%s` could not be loaded" filename))
         nil)))

(defn- develop-pre-req-1
  "Scan configuration items at the root of `configurations`.
  Configuration with no pre-reqs are not modified,
  For each configuration with pre-reqs, it is added as a `graph-deps` so the dependency will be loaded before this item,
  and added as new items in the root of the configurations"
  [configurations]
  (->> configurations
       (mapcat (fn [[cfg-item cfg-item-val]]
                 (let [pre-reqs (:pre-reqs cfg-item-val)]
                   (if (nil? pre-reqs)
                     [[cfg-item cfg-item-val]]
                     (let [deps-name (vec (keys pre-reqs))
                           new-deps-name (-> cfg-item-val
                                             (dissoc :pre-reqs)
                                             (merge {::graph-deps deps-name}))]
                       (concat [[cfg-item new-deps-name]]
                               (:pre-reqs cfg-item-val)))))))
       (into {})))

(defn- develop-pre-reqs
  "Take the configuration pre requisite and add it to the root of the graph"
  [configurations]
  (loop [configurations configurations
         max-loops 10]
    (let [updated-configurations (develop-pre-req-1 configurations)]
      (if (= updated-configurations configurations)
        configurations
        (if (pos? max-loops)
          (recur updated-configurations (dec max-loops))
          updated-configurations)))))

(defn- validate-cfg
  "Print message if the file content does not comply the schema"
  [file-content]
  (if (m/validate apps-schema file-content)
    (println "Configuration is valid")
    (do (println "Error in the configuration:")
        (println (->> file-content
                      (m/explain apps-schema)
                      me/humanize))))
  file-content)

(defn select-cfg-item
  "Select among the configurations the `cfg` one
  Params:
  * `configurations` configurations registry
  * `cfg` keyword of the configuration to select"
  [configurations cfg]
  (let [res (select-keys configurations
                         [cfg])]
    (if (nil? res)
      (println (format "Key %s not found, choose among: \n%s"
                       cfg
                       (keys configurations)))
      res)))

(defn read-configuration
  "Read the merged configuration of what is necessary and how it is done for each os
  Params:
  * `os` keyword among (:macos, :ubuntu)"
  [os cfg-item]
  (when cfg-item
    (println (format "Limited to configuration item `%s`" cfg-item)))
  (let [configurations (-> (read-data-as-resource "cfg_item.edn")
                           validate-cfg
                           (utils/deep-merge (->> os
                                                  cfg-envs
                                                  (format "%s/%s.edn" cfg-dir)
                                                  read-data-as-resource)))
        configurations (-> configurations
                           develop-pre-reqs
                           expand-pre-built)
        configurations (if (nil? cfg-item)
                         configurations
                         (select-cfg-item configurations cfg-item))
        seq-cfg (-> configurations
                    (deps-graph/build-from ::graph-deps)
                    deps-graph/topological-sort)
        res (->> seq-cfg
                 (mapcat (fn [k] [k (get configurations k)]))
                 (apply array-map))]
    (spit ".full_cfg_items.clj"
          res)
    res))

(comment
  (println (pr-str (read-configuration :ubuntu nil)))
  (println (pr-str (read-configuration :macos nil)))
  ;
  )
