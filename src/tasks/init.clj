(ns tasks.init
  (:require [cmds]
            [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init
  "Instal
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item sandbox?]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:init v))))]
    (println (format "Init following tasks `%s`" (mapv first cfg-items)))
    (doseq [cfg-item cfg-items]
      (when-let [[cfg-item {:keys [init]}] cfg-item]
        (println (format "Init `%s`" cfg-item))
        (cmds/execute-cmds init sandbox?)))))

(comment
  (init :macos nil true)
  ;
)
