(ns cfg-items
  "Configuration items management"
  (:require [utils]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

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
        (assoc-concat :check [["pip3" "check" package]]))
    cfg-item-val))

(defn- brew-update-cfg-item
  [{:keys [tap formula], :as cfg-item-val}]
  (if (some? formula)
    (-> cfg-item-val
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

(defn- develop-prerequisite-1
  [configurations]
  (->> configurations
       (mapcat (fn [[k v]]
                 (concat [[k (dissoc v :prerequisites)]] (:prerequisites v))))
       (into {})))

(defn- develop-prerequisites
  [configurations]
  (loop [configurations configurations
         max-loops 10]
    (let [updated-configurations (develop-prerequisite-1 configurations)]
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
         process-types
         develop-prerequisites)))

(comment
  (println (read-configuration :macos :emacs))
  ;
)
