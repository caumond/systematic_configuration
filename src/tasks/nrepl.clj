(ns tasks.nrepl
  (:require [babashka.fs :as fs]
            [babashka.nrepl.server :as srv]))

(def nrepl-file ".nrepl-port")

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn nrepl
  "Launch a repl locally"
  [_os _ _]
  (try (srv/start-server! {:host "localhost", :port 1339})
       (spit nrepl-file "1339")
       (-> (Runtime/getRuntime)
           (.addShutdownHook (Thread. (fn [] (fs/delete ".nrepl-port")))))
       (deref (promise))
       (catch Exception e
         (when (fs/exists? nrepl-file) (fs/delete nrepl-file))
         (println "Impossible to start the nrepl" (ex-message e)))))
