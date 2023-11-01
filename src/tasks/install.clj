(ns tasks.install
  (:require [cmds]
            [cfg-items]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn install
  "Instal
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:install v))))]
    (println (format "Install following tasks `%s`" (mapv first cfg-items)))
    (doseq [cfg-item cfg-items]
      (when-let [[cfg-item {:keys [install]}] cfg-item]
        (println (format "Install `%s`" cfg-item))
        (cmds/execute-cmds install)))))

(comment
  (install :macos nil)
  ;
)
