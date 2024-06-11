(ns cli-opts
  (:require [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-h" "--help" "Displays this help"]
   ["-s" "--sandbox" "Don't execute the commands, just sandbox them"]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn parse [args] (parse-opts args cli-options))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn do-opts
  [cli-opts]
  (when (get-in cli-opts [:options :help])
    (println "Usage is:")
    (println (:summary cli-opts))
    (System/exit 0)))
