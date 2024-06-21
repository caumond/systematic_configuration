(ns dag-test
  (:require [dag :as sut]
            [dag.map :as dag-map]
            [clojure.test :refer [deftest is]]))

(deftest topological-layers-test
  (is (= nil
         (->> {:c {:edges []}, :a {:edges []}}
              (dag-map/validate-simple :edges))
         (->> {:c {:edges [:d]},
               :d {:edges []},
               :a {:edges [:b :c]},
               :b {:edges [:c]},
               :h {:edges [:c]}}
              (dag-map/validate-simple :edges)))
      "All below examples are valid simple dag with a map.")
  (is
   (= [#{:a :c}]
      (-> (sut/topological-layers {:c {:edges []}, :a {:edges []}}
                                  (dag-map/simple :edges)
                                  1)
          :sorted))
   "When no edge has a successor, the nodes names are all returned, ordered.")
  (is (= [#{:d} #{:c}]
         (-> (sut/topological-layers {:c {:edges [:d]}, :d {:edges []}}
                                     (dag-map/simple :edges)
                                     3)
             :sorted))
      "Simple layers are found")
  (is (= nil
         (sut/topological-layers {:c {:edges [:a]}, :a {:edges []}}
                                 (dag-map/simple :edges)
                                 1))
      "Return nil if maximum iteration is reached first.")
  (is (= [#{:d} #{:c} #{:h :b} #{:a}]
         (-> (sut/topological-layers {:c {:edges [:d]},
                                      :d {:edges []},
                                      :a {:edges [:b :c]},
                                      :b {:edges [:c]},
                                      :h {:edges [:c]}}
                                     (dag-map/simple :edges)
                                     8)
             :sorted))
      "Complex layers are found.")
  (is (= [#{:d} #{:c} #{:h :b} #{:a}]
         (-> (sut/topological-layers {:c {:edges [:d]},
                                      :d {:edges []},
                                      :a {:edges [:b :c]},
                                      :b {:edges [:c]},
                                      :h {:edges [:c]}}
                                     (dag-map/simple :edges)
                                     8)
             :sorted))
      "Complex layers are found."))

(deftest ordered-nodes-test
  (let [v (->> [#{:d} #{:c} #{:h :b} #{:a}]
               (sut/ordered-nodes
                {:a 1, :b 2, :c 3, :d 4, :e 5, :f 6, :g 7, :h 8}))]
    (is (or (= {:d 4, :c 3, :h 8, :b 2, :a 1} v)
            (= {:d 4, :c 3, :b 2, :h 8, :a 1} v)))))
