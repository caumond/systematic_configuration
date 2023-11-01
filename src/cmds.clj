(ns cmds
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]))

(defn- execute-process
  [cmd sandbox?]
  (println (format "Command %s" (pr-str cmd)))
  (when-not sandbox?
    (if (or (empty? cmd) (not (every? string? cmd)))
      (println "Malformed command")
      (let [cmd (str/join " " cmd)
            {:keys [exit], :as res-cmd, :or {exit -1}}
              (when cmd (shell {:continue true} cmd))
            new-res res-cmd]
        (when-not (zero? exit) (println "Error during execution"))
        (if (nil? cmd) res-cmd new-res)))))

(defn- execute-processes
  [cmds fail-fast? sandbox?]
  (loop [cmds cmds
         res []]
    (let [cmd (first cmds)
          new-res (execute-process cmd sandbox?)
          {:keys [exit]} new-res]
      (cond (nil? new-res) nil
            (empty? (rest cmds)) new-res
            (nil? cmd) (recur (rest cmds) res)
            (or (zero? exit) (not fail-fast?)) (recur (rest cmds) new-res)
            :else (when-not (empty? cmds)
                    (println "Skipping next commands"))))))

(defn execute-cmds
  [cmds sandbox?]
  (mapv (comp :out) (execute-processes cmds false sandbox?)))

(defn execute-cmd
  [cmd sandbox?]
  (-> (execute-process cmd sandbox?)
      :out))

(defn execute-cmds-fail-fast
  [cmds sandbox?]
  (execute-processes cmds true sandbox?))

(comment
  (execute-cmd ["doom" "clean"] true)
  ;
)
