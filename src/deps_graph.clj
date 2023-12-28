(ns deps-graph
  "The dependency graph allow to execute tasks in the appropriate order")

(defn build-from
  "Build a map associating a key of `cfg-items` with the value of `kw`"
  [cfg-items kw]
  (->> cfg-items
       (map (fn [[k v]] [k (get v kw)]))
       (into {})))

(defn topological-sort
  [graph-deps]
  (loop [n 0
         sorted-deps []
         graph-deps graph-deps]
    (let [leaves (->> graph-deps
                      (filter (fn [[_ v]] (some? v)))
                      (mapv first)
                      sort)
          new-sorted-deps (vec (concat sorted-deps leaves))
          new-graph-deps (->> (apply dissoc graph-deps leaves)
                              (mapv (fn [[k v]] [k (remove (set leaves) v)]))
                              (into {}))]
      (if (empty? leaves)
        (vec new-sorted-deps)
        (when (< n 10) (recur (inc n) new-sorted-deps new-graph-deps))))))

;; (comment
;;   (require '[cfg-items :as cfg-items])
;;   (-> (cfg-items/read-configuration :macos nil)
;;       (build-from ::cfg-items/graph-deps)
;;       #_topological-sort)
;;   ;
;;   )
