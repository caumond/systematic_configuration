(ns ncmds
  "Execute commands."
  (:refer-clojure :exclude [print])
  (:require
   [babashka.process :refer [shell]]
   [clojure.string :as str]
   [malli.core :as m]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn to-str [cmd] (str/join " " cmd))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn execute-cmds
  "Execute commands.
  Return `nil` if all are successful.
  Stops on the first failing and returns a map with the error if it is failing."
  [cmds]
  (->> cmds
       (map execute-cmd)
       (filter some?)
       first))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn execute-all-cmds
  "Execute all processes, return a sequence of map with the error if it is failing, `nil` if success."
  [cmds]
  (->> cmds
       (map execute-cmd)
       (filterv some?)))
