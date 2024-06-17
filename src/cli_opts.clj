(ns cli-opts
  (:require [clojure.tools.cli :refer [parse-opts]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def cli-options
  [["-h" "--help" "Displays this help"]
   ["-s" "--sandbox" "Don't execute the commands, just sandbox them"]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn parse [args cli-opts] (parse-opts args cli-opts))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn print-summary
  [cli-opts]
  (when (get-in cli-opts [:options :help])
    (println "Usage is:")
    (println (:summary cli-opts))
    (System/exit 0)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn valid-summary
  [cli-opts]
  (when (:errors cli-opts)
    (apply println "invalid options: " (:errors cli-opts))
    (System/exit -1)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn sandbox?
  [cli-opts]
  (boolean (get-in cli-opts [:options :sandbox])))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn cli-args
  [cli-opts]
  (->> cli-opts
       :arguments
       (mapv keyword)))
