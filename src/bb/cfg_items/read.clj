(ns cfg-items.read
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn read-data-as-resource
  [filename]
  (try (->> filename
            io/resource
            slurp
            edn/read-string)
       (catch Exception _
         (println (format "File `%s` could not be loaded" filename))
         nil)))
