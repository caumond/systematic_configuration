(ns tasks.format
  (:refer-clojure :exclude [format])
  (:require [cli-opts :refer [parsed-cli-opts]]
            [ncmds]))

(def cli-opt-vals
  [["-s" "--sandbox"
    "In sandbox no command is executed so configuration is not modified, just what would be done is."]
   ["-e" "--exception" "See details of the exception."]])

(defn format
  [cli-args on-error-code]
  (let [parsed-cli-opts (parsed-cli-opts cli-args cli-opt-vals on-error-code)
        error-maps
        (ncmds/execute-all-cmds
         [["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w" "{}"]]
         {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                             (println "Cmd : " %)
                             (cli-opts/get parsed-cli-opts :verbose)
                             (println "Executing: " %)),
          :sandbox? (cli-opts/get parsed-cli-opts :sandbox),
          :post-cmd-fn (fn [cmd error-map]
                         (when-let [error-code (:error-code error-map)]
                           (on-error-code (format "Command `%s` with error" cmd)
                                          error-code)))})]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))
