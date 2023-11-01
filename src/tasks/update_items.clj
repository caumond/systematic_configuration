(ns tasks.update-items
  (:require [cfg-items]
            [cmds]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn update-items
  "Update our toolings"
  [os cfg-item sandbox?]
  (println "Update toolings")
  (let [configurations (->> (cfg-items/read-configuration os cfg-item)
                            (filter (fn [[_ v]] (:update v))))]
    (println (format "Found configurations %s" (mapv first configurations)))
    (doseq [[cfg-item {:keys [update]}] configurations]
      (println (format "Execute `%s`" cfg-item))
      (cmds/execute-cmds-fail-fast update sandbox?))))

(comment
  (update-items :macos nil true)
  ;
)
