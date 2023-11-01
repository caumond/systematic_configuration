(ns cfg-items
  "Configuration items management"
  (:require
   [utils]
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(defn- read-data-as-resource
  [filename]
  (try
    (->> filename
         io/resource
         slurp
         edn/read-string)
    (catch Exception _
      (println (format "File `%s` could not be loaded"
                       filename))
      nil)))

(def ^:private cfg-dir
  "Where configuration per os are stored"
  "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos"
   :ubuntu "ubuntu"})

(defn read-configuration
  "Read the merged configuration of what is necessary and how it is done for each os
  Params:
  * `os` keyword among (:macos, :ubuntu)"
  [os]
  (utils/deep-merge (read-data-as-resource "cfg_item.edn")
                    (->> os
                         cfg-envs
                         (format "%s/%s.edn" cfg-dir)
                         read-data-as-resource)))

(read-configuration :macos)
