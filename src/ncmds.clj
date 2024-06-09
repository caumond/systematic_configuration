(ns ncmds
  "Execute commands."
  (:refer-clojure :exclude [print])
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]))

(defn cmd-to-str [cmd] (str/join " " cmd))

(defn print
  [cmds]
  (doseq [cmd cmds]
    (-> cmd
        cmd-to-str
        println)))

(defn- execute-process
  [cmd]
  (println (format "Execute `%s`" (pr-str (cmd-to-str cmd))))
  (when-not (or (empty? cmd) (not (every? string? cmd)))
    (let [cmd (cmd-to-str cmd)
          {:keys [exit], :as res-cmd, :or {exit -1}}
          (when cmd (shell {:continue true} cmd))
          new-res res-cmd]
      (when-not (zero? exit) (println "Error during execution"))
      (if (nil? cmd) res-cmd new-res))))

(defn- execute-processes
  [cmds fail-fast?]
  (loop [cmds cmds
         res []]
    (let [cmd (first cmds)
          new-res (execute-process cmd)
          {:keys [exit]} new-res]
      (cond (nil? new-res) nil
            (empty? (rest cmds)) new-res
            (nil? cmd) (recur (rest cmds) res)
            (or (zero? exit) (not fail-fast?)) (recur (rest cmds) new-res)
            :else (when-not (empty? cmds)
                    (println "Skipping next commands"))))))

(defn execute-cmds [cmds] (mapv (comp :out) (execute-processes cmds false)))

(defn execute-cmd
  [cmd]
  (-> (execute-process cmd)
      :out))

(defn execute-cmds-fail-fast
  [cmds]
  (mapv (comp :out) (execute-processes cmds true)))
