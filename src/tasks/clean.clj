(ns tasks.clean
  (:require [cmds]
            [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn clean
  "Clean all cache data
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-items sandbox?]
  (println "Clean toolings")
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-items)
                       (filter (fn [[_ v]] (:clean v))))]
    (println (format "Clean following tasks `%s`" (mapv first cfg-items)))
    (doseq [cfg-item cfg-items]
      (when-let [[cfg-item {:keys [clean]}] cfg-item]
        (println (format "Clean `%s`" cfg-item))
        (cmds/execute-cmds clean sandbox?)))))

(comment
  (clean :macos nil false)
  ;
)
