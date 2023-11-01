(ns tasks.check
  (:require [cmds]
            [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn check
  "Check
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:check v))))
        _ (println (format "Check following tasks `%s`" (mapv first cfg-items)))
        res (for [cfg-item cfg-items]
              (when-let [[cfg-item {:keys [check]}] cfg-item]
                (println (format "Check `%s`" cfg-item))
                (cmds/execute-cmds check)))]
    (when (some true? (map boolean res)) (System/exit -1))))
(comment
  (check :macos :brew)
  ;
)
