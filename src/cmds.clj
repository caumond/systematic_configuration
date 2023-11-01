(ns cmds
  (:require
   [clojure.string :as str]
   [babashka.process :refer [shell]]))

(defn- execute-process
  [cmd]
  (let [_ (println (format "Command %s" (pr-str cmd)))]

    (if (not (every? string? cmd))
      (println "Malformed command")
      (let [cmd (str/join " " cmd)
            {:keys [exit]
             :as res-cmd
             :or {exit -1}} (when cmd
                              (shell cmd))
            new-res res-cmd]
        (when-not (zero? exit)
          (println "Error during execution")
          (println "result is: " res-cmd))
        (if (nil? cmd) res-cmd
            new-res)))))

(defn- execute-processes
  [cmds fail-fast?]
  (loop [cmds cmds
         res []]
    (let [cmd (first cmds)
          new-res (execute-process cmd)
          {:keys [exit]} new-res]
      (cond
        (empty? (rest cmds)) new-res
        (nil? cmd) (recur (rest cmds)
                          res)
        (or (zero? exit)
            (not fail-fast?)) (recur (rest cmds)
                                     new-res)
        :else (when-not (empty? cmds)
                (println "Skipping next commands"))))))

(defn execute-cmds
  [cmds]
  (mapv (comp :out)
        (execute-processes cmds false)))

(defn execute-cmd
  [cmd]
  (-> (execute-process cmd)
      :out))

(defn execute-cmds-fail-fast
  [cmds]
  (execute-processes cmds true))
