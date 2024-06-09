(ns tasks.check
  (:require [cmds]
            [cfg-items]))

(defn check
  "Check
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item sandbox?]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:check v))))
        _ (println (format "Check following tasks `%s`" (mapv first cfg-items)))
        res (vec (for [cfg-item cfg-items]
                   (when-let [[cfg-item {:keys [check]}] cfg-item]
                     (println (format "Check `%s`" cfg-item))
                     (cmds/execute-cmds check sandbox?))))]
    (when (some true? (map boolean res)) (System/exit -1))))

(defn ncheck
  "Check
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os]
  (->> (cfg-items/read-all-os-configuration os)
       (filter (fn [[_ v]] (:check v)))
       (mapcat (fn [[cfg-item {:keys [check]}]] (when cfg-item check)))))

(comment
  (ncheck :macos)
  ;
)
