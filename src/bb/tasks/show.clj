(ns tasks.show
  (:require [cfg-items]
            [cli-opts :refer [parsed-cli-opts]]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def cli-opts-vals
  ["-o" "--os OS"
   "In sandbox no command is executed so configuration is not modified, just what would be done is."
   :validate [string? "Must be a string."]])

(defn show
  [cli-args on-error-code]
  (when-let [parsed-cli-opts
             (parsed-cli-opts cli-args cli-opts-vals on-error-code)]
    (let [cfg-items (->> (cli-opts/get parsed-cli-opts :os)
                         (cfg-items/build parsed-cli-opts))
          {:keys [cycle-detected subgraph-with-cycle sorted],
           :as cfg-items-by-layers}
          (cfg-items/cfg-items-by-layers cfg-items)]
      ;;  (pp/pprint cfg-items)
      (when (cli-opts/get parsed-cli-opts :verbose)
        (println "Sorted:")
        (pp/pprint (cfg-items/ordered-cfg-items cfg-items cfg-items-by-layers)))
      (when cycle-detected
        (println "Setup is invalid - a cycle has been detected.")
        (println "subgraph-with-cycle: " (keys subgraph-with-cycle)))
      (run! println
            (->> (interleave (range) sorted)
                 (partition 2)
                 (map (partial str/join " -> "))))
      nil)))
