(ns tasks.version
  "Version of configuration-items."
  (:require [cmds]
            [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn version
  "Version
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item sandbox?]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:version v))))]
    (println (format "Version following tasks `%s`" (mapv first cfg-items)))
    (doseq [cfg-item cfg-items]
      (when-let [[cfg-item {:keys [version]}] cfg-item]
        (println (format "Version `%s`" cfg-item))
        (cmds/execute-cmd version sandbox?)))))

(defn nversion
  "Returns."
  [os]
  (->> (cfg-items/read-all-os-configuration os)
       (filter (fn [[_ v]] (:version v)))
       (mapv (fn [cfg-item]
               (when-let [[_ {:keys [version]}] cfg-item] version)))))

(comment
  (version :macos nil true)
  ;
)
