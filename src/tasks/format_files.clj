(ns tasks.format-files
  (:require [cmds]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn format-files
  "Format-Files
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [_os _ sandbox?]
  (cmds/execute-cmd ["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w {}"]
                    sandbox?))

(comment
  (format-files :macos nil true)
  ;
)
