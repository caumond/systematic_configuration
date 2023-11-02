(ns cfg-items
  "Configuration items management"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [deps-graph]
            [utils]))

(def ^:private cfg-dir "Where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn- assoc-concat [val kw coll] (update val kw #(concat coll %)))

(defn- pip-update-cfg-item
  [{:keys [package], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? package)
           (-> cfg-item-val
               (assoc-concat :install [["pip3" "install" package]])
               (assoc-concat :update [["pip3" "install" "--upgrade" package]])
               (assoc-concat ::graph-deps [:pip])
               (assoc-concat :check [["pip3" "check" package]])))))

(defn- brew-update-cfg-item
  [{:keys [tap formula], :as cfg-item-val}]
  (merge cfg-item-val
         (when (some? formula)
           (-> cfg-item-val
               (assoc-concat ::graph-deps [:brew])
               (assoc-concat :install
                             (concat (when tap [["brew" "tap" tap]])
                                     [["brew" "install" formula]]))
               (assoc-concat :update [["brew" "upgrade" formula]])))))

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
       (mapv (fn [[cfg-item val]] [cfg-item
                                   (-> val
                                       brew-update-cfg-item
                                       pip-update-cfg-item
                                       tmp-dirs-cfg-item
                                       npm-cfg-item
                                       tmp-files-cfg-item)]))
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
  [configurations]
  (->> configurations
       (mapcat (fn [[cfg-item cfg-item-val]]
                 (let [deps-name (vec (keys (:pre-reqs cfg-item-val)))
                       new-deps-name (-> cfg-item-val
                                         (dissoc :pre-reqs)
                                         (merge (when-not (empty? deps-name)
                                                  {::graph-deps deps-name})))]
                   (concat [[cfg-item new-deps-name]]
                           (:pre-reqs cfg-item-val)))))
       (into {})))

(defn- develop-pre-reqs
  [configurations]
  (loop [configurations configurations
         max-loops 10]
    (let [updated-configurations (develop-pre-req-1 configurations)]
      (if (= updated-configurations configurations)
        configurations
        (if (pos? max-loops)
          (recur updated-configurations (dec max-loops))
          updated-configurations)))))

(defn read-configuration
  "Read the merged configuration of what is necessary and how it is done for each os
  Params:
  * `os` keyword among (:macos, :ubuntu)"
  [os cfg-item]
  (when cfg-item
    (println (format "Limited to configuration item `%s`" cfg-item)))
  (let [configurations (utils/deep-merge (read-data-as-resource "cfg_item.edn")
                                         (->> os
                                              cfg-envs
                                              (format "%s/%s.edn" cfg-dir)
                                              read-data-as-resource))
        configurations (-> (if (nil? cfg-item)
                               configurations
                               (select-keys configurations [cfg-item]))
                           develop-pre-reqs
                           expand-pre-built)
        seq-cfg (-> configurations
                    (deps-graph/build-from ::graph-deps)
                    deps-graph/topological-sort)]
    (->> seq-cfg
         (mapcat (fn [k] [k (get configurations k)]))
         (apply array-map))))

(comment
  (println (pr-str (read-configuration :macos nil)))
  ;
)
