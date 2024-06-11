(ns deps-graph "Dependency graph.")

(defn topological-layers
  "Sort the graph topologically, so nodes are grouped in an ordered collection of node-name, in such a way that a node in layer number `n` has no successor in layers after `n`.

  The graph is assumed to be a direct acyclic graph, with oriented edges. It is accessed through the graph manipulation functions.

  The iterations are limited to `max-iteration` to limit endless loops.

  Use flatten on it to turn it into a topologically ordered list of nodes."
  [dag {:keys [dag-nodes node-names node-edges remove-nodes remove-successors]}
   max-iteration]
  (loop [n 0
         sorted-nodes []
         dag dag]
    (println "dag: " dag)
    (let [nodes-wo-successors (->> dag
                                   dag-nodes
                                   (filter (comp empty? node-edges)))]
      (if (empty? nodes-wo-successors)
        (vec sorted-nodes)
        (when (< n max-iteration)
          (recur (inc n)
                 (conj sorted-nodes (node-names nodes-wo-successors))
                 (-> dag
                     (remove-nodes nodes-wo-successors)
                     (remove-successors nodes-wo-successors))))))))
