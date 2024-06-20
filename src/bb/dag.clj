(ns dag "Direct Acyclic Graph.")

(defn topological-layers
  "Returns the topological layers of graph `dag`.

  Let's consider the following `dag`:
  ```clojure
  {:c {:edges [:d]},
   :d {:edges []},
   :a {:edges [:b :c]},
   :b {:edges [:c]},
   :h {:edges [:c]}}
  ```
  The topological layers is an ordered list of set of node names:
  ```clojure
  [#{:d} #{:c} #{:h :b} #{:a}]
  ```

  The first element contains the names of the nodes with no successor. Here it means `:d` is the only node with no successor in the graph.

  The graph is assumed to be a direct acyclic graph, with oriented edges. It is accessed through the `graph-manipulator` functions.

  In case the graph is by mistake cyclic, the iterations are limited to `max-iteration` to limit endless loops."
  [dag
   {:keys [dag-nodes node-names node-edges remove-nodes remove-successors],
    :as _graph-manipulator} max-iteration]
  (loop [n 0
         sorted-nodes []
         dag dag]
    (let [nodes-wo-successors (->> dag
                                   dag-nodes
                                   (filter (comp empty? node-edges)))
          nodes-wo-successors-names (node-names nodes-wo-successors)]
      (cond (empty? dag) {:cycle-detected false, :sorted (vec sorted-nodes)}
            (empty? nodes-wo-successors) {:cycle-detected true,
                                          :sorted (vec sorted-nodes),
                                          :subgraph-with-cycle dag}
            :else (when (< n max-iteration)
                    (recur (inc n)
                           (conj sorted-nodes nodes-wo-successors-names)
                           (-> dag
                               (remove-nodes nodes-wo-successors-names)
                               (remove-successors
                                nodes-wo-successors-names))))))))

(defn ordered-nodes
  "Turns a topological layers into an ordered list of nodes"
  [dag topological-layers]
  (select-keys dag (vec (flatten (map vec topological-layers)))))
