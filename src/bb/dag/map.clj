(ns dag.map
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
  [kw node]
  (-> node
      second
      kw
      set))

(defn- humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(defn validate-simple
  [edge-kw dag]
  (->> dag
       (humanize [:map-of :keyword
                  [:map {:closed false} [edge-kw [:sequential :any]]]])))

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
  [edge-kw dag node-names]
  (-> dag
      (update-vals (fn [x]
                     (update x
                             edge-kw
                             (fn [edge]
                               (->> edge
                                    (remove (set node-names))
                                    vec)))))))

(defn simple
  "For a dag which is a map, "
  [edge-kw]
  {:dag-nodes dag-nodes,
   :node-edges (partial node-edges edge-kw),
   :node-names node-names,
   :valid (partial validate-simple edge-kw),
   :remove-nodes remove-nodes,
   :remove-successors (partial remove-successors edge-kw)})
