(ns cfg-items
  "Configuration items management"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [utils]))

(def ^:private cfg-dir "Where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn- assoc-concat [val kw coll] (update val kw #(concat coll %)))

(defn- pip-update-cfg-item
  [{:keys [package], :as cfg-item-val}]
  (if (some? package)
    (-> cfg-item-val
        (assoc-concat :install [["pip3" "install" package]])
        (assoc-concat :update [["pip3" "install" "--upgrade" package]])
        (assoc-concat ::graph-deps [:pip])
        (assoc-concat :check [["pip3" "check" package]]))
    cfg-item-val))

(defn- brew-update-cfg-item
  [{:keys [tap formula], :as cfg-item-val}]
  (if (some? formula)
    (-> cfg-item-val
        (assoc-concat ::graph-deps [:brew])
        (assoc-concat :install
                      (concat (when tap [["brew" "tap" tap]])
                              [["brew" "install" formula]]))
        (assoc-concat :update [["brew" "upgrade" formula]]))
    cfg-item-val))

(defn- process-types
  "For each predefined type"
  [configurations]
  (->> configurations
       (mapv (fn [[cfg-item val]] [cfg-item
                                   (-> val
                                       brew-update-cfg-item
                                       pip-update-cfg-item)]))))

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
        configurations (if (nil? cfg-item)
                         configurations
                         (select-keys configurations [cfg-item]))]
    (->> configurations
         develop-pre-reqs
         process-types)))

