(ns test-runner
  (:require [clojure.test :as t]
            [babashka.classpath :as cp]))

(defn exec
  []
  (let [path ['cfg-items.cmds-test 'dag.map-test 'cfg-items-schema-test
              'cfg-items-test 'dag-test 'ncmds-test]]
    (cp/add-classpath "src/bb:test/bb")
    (apply require path)
    (let [test-results (apply t/run-tests path)
          {:keys [fail error]} test-results]
      (when (pos? (+ fail error)) (System/exit 1)))))
