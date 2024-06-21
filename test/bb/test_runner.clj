(ns test-runner
  (:require [clojure.test :as t]
            [babashka.classpath :as cp]))

(defn exec
  []
  (cp/add-classpath "src/bb:test/bb")
  (require 'dag.map-test 'cfg-items-test 'dag-test 'ncmds-test)
  (let [test-results
        (t/run-tests 'dag.map-test 'cfg-items-test 'dag-test 'ncmds-test)]
    (let [{:keys [fail error]} test-results]
      (when (pos? (+ fail error)) (System/exit 1)))))
