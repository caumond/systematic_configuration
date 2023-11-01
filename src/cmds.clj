(ns cmds
  (:require
   [clojure.string :as str]
   [babashka.process :refer [shell]]))

(defn- exec-process
  [cmds fail-fast?]
  (loop [cmds cmds
         res []]
    (let [cmd  (str/join " " (first cmds))
          _ (println (format "Command %s" cmd))
          {:keys [exit]
           :as res-cmd
           :or {exit -1}} (shell cmd)
          new-res (conj res res-cmd)]
      (when-not (zero? exit)
        (println "Error during execution")
        (println "result is: " res-cmd))
      (cond
        (empty? (rest cmds)) new-res
        (or (zero? exit)
            (not fail-fast?)) (recur (rest cmds)
                                     new-res)
        :else (when-not (empty? cmds)
                (println "Skipping next commands"))))))

(defn- execute*
  [cmds fail-fast?]
  (exec-process cmds fail-fast?))

(defn execute-cmds
  [cmds]
  (mapv (comp :out)
        (execute* cmds false)))

(defn execute-cmds-fail-fast
  [cmds]
  (execute* cmds true))
