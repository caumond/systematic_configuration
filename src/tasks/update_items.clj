(ns tasks.update-items
  (:require
   [cfg-items]
   [cmds]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn update-items
  "Update our toolings"
  [os]
  (println "Update toolings")
  (let [configurations (cfg-items/read-configuration os)]
    (println (format "Found configurations %s" (vec (keys configurations))))
    (doseq [[cfg-item {:keys [update]}] configurations]
      (println (format "Execute `%s`" cfg-item))
      (cmds/execute-cmds-fail-fast update))))

(comment
  (update-items :macos)
;
 )
