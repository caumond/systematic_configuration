(ns tasks.list-cfg-items
  (:require [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn list-cfg-items
  "List
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item _]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:install v))))]
    (println (format "These are the existig tasks `%s`" (pr-str (mapv first cfg-items))))))

(comment
  (list-cfg-items :macos nil true)
  ;
  )
