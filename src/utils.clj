(ns utils)

(defn deep-merge
  "Deep merge nested maps.
  Last map has higher priority

  This code comes from this [gist](https://gist.github.com/danielpcox/c70a8aa2c36766200a95)"
  [& maps]
  (apply merge-with
    (fn [& args]
      (if (every? #(or (map? %) (nil? %)) args)
        (apply deep-merge args)
        (last args)))
    maps))
