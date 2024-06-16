(ns deps-graph.map-test
  (:require [deps-graph.map :as sut]
            [clojure.test :refer [deftest is]]))

(deftest dag-nodes-test
  (is (= {:c {:edges [:d]}, :a {:edges []}}
         (-> {:c {:edges [:d]}, :a {:edges []}}
             sut/dag-nodes))
      "Keys of map are returned into a set of node names."))

(deftest node-edges-test
  (is (= #{:d}
         (-> {:c {:edges [:d]}, :a {:edges []}}
             first
             sut/node-edges))
      "Node names are returned into a set."))

(deftest validate-simple-test
  (is (= nil (sut/validate-simple {:dd {:edges [{:foo :bar}]}})))
  (is (some? (sut/validate-simple {"dd" 12})))
  (is (some? (sut/validate-simple []))))

(deftest remove-nodes-test
  (is (= {:c {:edges [:d]}}
         (-> {:c {:edges [:d]}, :a {:edges []}}
             (sut/remove-nodes (sut/node-names {:a {:foo :bar}}))))))

(deftest remove-successors-test
  (is (= {:c {:edges [:q :d]}, :b {:edges [:d]}, :a {:edges []}}
         (-> {:c {:edges [:q :d :a]}, :b {:edges [:d :a]}, :a {:edges []}}
             (sut/remove-successors [:a])))
      "Node names are returned into a set."))

(deftest assembly-test
  (let [dag {:c {:edges [:d]}, :a {:edges []}}
        nodes (-> dag
                  sut/dag-nodes)]
    (is (= {} (sut/remove-nodes dag nodes))
        "Removing nodes through nodes is ok."))
  (let [dag {:c {:edges [:d]}, :a {:edges []}}
        nodes (-> dag
                  sut/dag-nodes)]
    (is (= {}
           (-> dag
               (sut/remove-nodes nodes)
               (sut/remove-successors nodes)))
        "Removing nodes through nodes is ok.")))
