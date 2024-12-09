(ns cli-opts
  (:refer-clojure :exclude [get])
  (:require [clojure.tools.cli :refer [parse-opts]]
            [current-os]))

(def common-cli-opts
  [["-h" "--help" "Displays this help"] ["-v" "--verbose" "Verbose"]])

(defn parse [args cli-opts] (parse-opts args cli-opts))

(defn print-summary
  [parsed-cli-opts]
  (when (get-in parsed-cli-opts [:options :help])
    (println "Usage is:")
    (println (:summary parsed-cli-opts))
    {:summary-only? true}))

(defn valid-summary
  [parsed-cli-opts]
  (when (:errors parsed-cli-opts)
    (apply println "invalid options: " (:errors parsed-cli-opts))
    {:errors-found true}))

(defn get
  [parsed-cli-opts kw]
  (get-in parsed-cli-opts [:parsed-cli-opts :options kw]))

(defn cli-args
  [parsed-cli-opts]
  (->> parsed-cli-opts
       :arguments
       (mapv keyword)))

(defn cli-error-code
  "Returns the cli error code of the errors found in task-validation."
  [task-validation]
  (let [error-code (cond (:errors-found task-validation) 2
                         (:summary-only? task-validation) 0)]
    (cond-> task-validation error-code (assoc :error-code error-code))))

(defn validate-task
  "A task option is 100% defined so we can display the summary,
  add options"
  [cli-args task-cli-opts]
  (let [parsed-cli-opts (->> (concat common-cli-opts task-cli-opts)
                             (parse cli-args))]
    (-> (print-summary parsed-cli-opts)
        (merge (valid-summary parsed-cli-opts))
        (assoc :parsed-cli-opts parsed-cli-opts)
        cli-error-code)))

(defn- stop [n] (System/exit (or n 0)))

(defn parsed-cli-opts
  "Parse the `cli-opts` defined in this parameter and return it, except if an error occurs, so `(on-error-code err-code)` is called with `err-code` the exit code."
  [cli-args task-cli-opts on-error-code]
  (let [parsed-cli-opts (validate-task cli-args task-cli-opts)]
    (when (get parsed-cli-opts :verbose)
      (println "Verbose mode: ")
      (println "   Arguments: "
               (get-in parsed-cli-opts [:parsed-cli-opts :arguments]))
      (println "   Options: "
               (get-in parsed-cli-opts [:parsed-cli-opts :options])))
    (if-let [error-code (:error-code parsed-cli-opts)]
      (do (if (nil? on-error-code) (stop error-code) (on-error-code error-code))
          nil)
      parsed-cli-opts)))
