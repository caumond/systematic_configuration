(ns tasks.format-files
  "Format files with zprint."
  (:require [cmds]))

(def cmd ["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w {}"])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn format-files "Format-Files." [] (cmds/execute-ncmd cmd))

(comment
  (format-files)
  ;
)
