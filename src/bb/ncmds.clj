(ns ncmds
  "Execute commands."
  (:refer-clojure :exclude [print])
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]
            [malli.core :as m]
            [clojure.pprint :as pp]))

(defn to-str [cmd] (str/join " " cmd))

(defn validate-cmd [cmd] (malli.core/validate [:sequential :string] cmd))

(defn- execute-process*
  [cmd string?]
  (let [{:keys [exit], :as res, :or {exit -1}}
        (apply shell
               (cond-> {:continue true} string? (assoc :out :string))
               cmd)]
    (when-not (zero? exit)
      (println "Error during execution: Exit-code " exit)
      (println "      " cmd))
    res))

(defn execute-as-string
  [cmd]
  (try (-> cmd
           (execute-process* true)
           :out)
       (catch Exception _ nil)))

(defn execute-cmd
  "Helper for executing a command.
  Returns `nil` if succesful, an error map with
  * `:cmd` and `exit-code` keys in the map in case execution has failed in shell.
  * `:cmd` and `:exception` keys in the map in case the execution has not started."
  [cmd]
  (try (let [res (-> cmd
                     (execute-process* false))]
         (when-not (zero? (:exit res)) {:cmd cmd, :exit-code (:exit res)}))
       (catch Exception e {:cmd cmd, :exception e})))

(defn execute-all-cmds
  "Execute all processes, return a sequence of map with the error if it is failing, `nil` if success."
  ([cmds] (execute-all-cmds cmds {}))
  ([cmds {:keys [sandbox? pre-cmd-fn post-cmd-fn exception], :as _params}]
   (cond (empty? cmds) (println "nothing to do.")
         sandbox? (run! println (map ncmds/to-str cmds))
         :else
         (->> cmds
              (map (fn [cmd]
                     (try (when (fn? pre-cmd-fn) (pre-cmd-fn cmd))
                          (let [error-map (execute-cmd cmd)]
                            (when (fn? post-cmd-fn) (post-cmd-fn cmd error-map))
                            error-map)
                          (catch Exception e
                            (println
                             (format "Exception during execution of `%s`" cmd))
                            (throw e)))))
              (filterv some?)))))

(defn println-summary-errors
  [error-maps exception?]
  (when-not (empty? error-maps)
    (let [n (count error-maps)]
      (if (= 1 n)
        (println "One command has failed.")
        (println (format "%s commands have failed." n)))
      (run! println (map :cmd error-maps))
      (when exception?
        (println "Detailed errors:")
        (run! pp/pprint error-maps)))))

(defn merge-cmds
  "All cmd in the `cmds` collection that starts with `starts-with` and end with `ends-with` are transformed so
  ```clojure
  (sut/merge-cmds [[\"brew\" \"list\" \"--cask\" \"a\" \"b\" \"--versions\"]
                   [\"brew\" \"not-list\" \"--cask\" \"a\" \"c\" \"--versions\"]
                   [\"brew\" \"list\" \"a\" \"d\" \"--versions\"]]
                   [\"brew\" \"list\" \"--cask\"]
                   [\"--versions\"])
  ```
  returns
  ```clojure
  ```
  "
  [cmds starts-with ends-with pred-fn]
  (let [n (count starts-with)
        rn (count ends-with)
        {matching-cmds true, not-matching-cmds false}
        (->> cmds
             (group-by #(and (pred-fn %)
                             (= starts-with (take n %))
                             (= ends-with (take rn (reverse %))))))
        merged-params (mapcat #(nth (->> %
                                         (drop n)
                                         (iterate butlast))
                                    rn)
                       matching-cmds)]
    (-> (if (empty? merged-params)
            []
            (-> (concat starts-with merged-params ends-with)
                vec
                vector))
        (concat not-matching-cmds)
        vec)))
