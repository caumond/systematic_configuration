(ns tasks.nrepl
  "Launch a bb repl."
  (:require [babashka.fs :as fs]
            [cli-opts]
            [babashka.nrepl.server :as srv]))

(def nrepl-file ".nrepl-port")

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn nrepl
  "Launch a repl locally."
  [cli-args]
  (try (let [parsed-cli-opts (cli-opts/validate-task
                              cli-args
                              [["-p" "--port PORT" "Port for nrepl" :default
                                1339 :parse-fn #(Integer/parseInt %) :validate
                                [#(< 0 % 0x10000)
                                 "Must be a number between 0 and 65536"]]])
             port (cli-opts/get parsed-cli-opts :port)]
         (srv/start-server! {:host "localhost", :port port})
         (spit nrepl-file (str port))
         (-> (Runtime/getRuntime)
             (.addShutdownHook (Thread. (fn [] (fs/delete ".nrepl-port")))))
         (deref (promise)))
       (catch Exception e
         (when (fs/exists? nrepl-file) (fs/delete nrepl-file))
         (println "Impossible to start the nrepl" (ex-message e)))))
