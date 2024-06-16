(ns deps-graph.map
  "A map deps graph has one key for each node (`:a, :c` in the example below). For a node `n`, the value is a map where the key `:edges` is containing the list of following nodes.

  ```clojure
  {:c {:edges [:d]}, :a {:edges [:c]}}
  ```

  This example describes `:c->:d`, `:a->:c`."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def dag-nodes
  "List of nodes is the complete map. Get the names of them with node-name function."
  identity)

(defn node-edges
  "Returns a set the edges of `node`."
  [node]
  (-> node
      second
      :edges
      set))

(defn- humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(defn validate-simple
  [dag]
  (->> dag
       (humanize [:map-of :keyword
                  [:map {:closed false} [:edges [:sequential :any]]]])))

(defn node-names
  "With a graph or subgraph called `dag`, the names are the keys of it."
  [dag]
  (-> dag
      keys
      set))

(defn remove-nodes
  "Remove nodes from dag."
  [dag node-names]
  (apply dissoc dag node-names))

(defn remove-successors
  "Remove nodes from the list of "
  [dag node-names]
  (-> dag
      (update-vals (fn [x]
                     (update x
                             :edges
                             (fn [edge]
                               (->> edge
                                    (remove (set node-names))
                                    vec)))))))

(def simple
  "For a dag which is a map, "
  {:dag-nodes dag-nodes,
   :node-edges node-edges,
   :node-names node-names,
   :valid validate-simple,
   :remove-nodes remove-nodes,
   :remove-successors remove-successors})
