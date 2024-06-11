(ns deps-graph-test
  (:require [deps-graph :as sut]
            [deps-graph.map :as graph-map]
            [clojure.test :refer [deftest is]]))

(deftest topological-layers-test
  (is
   (= [#{:a :c}]
      (sut/topological-layers {:c {:edges []}, :a {:edges []}}
                              graph-map/simple
                              1))
   "When no edge has a successor, the nodes names are all returned, ordered.")
  (is (= [#{:d} #{:c}]
         (sut/topological-layers {:c {:edges [:d]}, :d {:edges []}}
                                 graph-map/simple
                                 3))
      "Simple layers are found")
  (is (= nil
         (sut/topological-layers {:c {:edges [:a]}, :a {:edges []}}
                                 graph-map/simple
                                 1))
      "Return nil if maximum iteration is reached first.")
  (is (= [#{:d} #{:c} #{:h :b} #{:a}]
         (sut/topological-layers {:c {:edges [:d]},
                                  :d {:edges []},
                                  :a {:edges [:b :c]},
                                  :b {:edges [:c]},
                                  :h {:edges [:c]}}
                                 graph-map/simple
                                 8))
      "Complex layers are found."))
