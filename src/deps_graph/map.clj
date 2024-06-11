(ns deps-graph.map
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(def dag-nodes identity)

(defn node-edges
  [node]
  (-> node
      second
      :edges
      set))

(defn remove-nodes [dag nodes] (apply dissoc dag (keys nodes)))

(defn remove-successors
  [dag nodes]
  (-> dag
      (update-vals (fn [x]
                     (update x
                             :edges
                             (fn [edge]
                               (->> edge
                                    (remove (set (keys nodes)))
                                    vec)))))))

(defn validate-simple
  [dag]
  (->> dag
       (humanize [:map-of :keyword
                  [:map {:closed false} [:edges [:sequential [:map]]]]])))

(defn node-names
  [dag]
  (-> dag
      keys
      set))

(def simple
  "For a dag which is a map, "
  {:dag-nodes dag-nodes,
   :node-edges node-edges,
   :node-names node-names,
   :valid validate-simple,
   :remove-nodes remove-nodes,
   :remove-successors remove-successors})
