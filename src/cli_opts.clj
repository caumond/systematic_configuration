(ns cli-opts
  (:require [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-h" "--help" "Displays this help"]
   ["-c" "--cfg-item CFG_ITEM" "Run only one configuration item"]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn parse [args] (parse-opts args cli-options))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn do-opts
  [cli-opts]
  (when (get-in cli-opts [:options :help])
    (println "Usage is:")
    (println (:summary cli-opts))
    (System/exit 0)))
