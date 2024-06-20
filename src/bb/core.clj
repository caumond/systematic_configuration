(ns core
  (:refer-clojure :exclude [format update])
  (:require [babashka.fs :as fs]
            [current-os]
            [clojure.pprint :as pp]
            [cfg-items]
            [ncmds]
            [cli-opts :as cli-opts]
            [clojure.string :as str]))

(def ^:private backup-dir "cfg_backup")

(defn- stop [n] (System/exit (or n 0)))

(def ^:private separator
  (apply str (conj (vec (repeat 10 "*"))
                   "\n")))

(def sandbox-cli-opt
  ["-s" "--sandbox"
   "In sandbox no command is executed so configuration is not modified, just what would be done is."])

(def ^:private cmd-execution-cli-opt
  ["-e" "--exception" "See details of the exception."])

(defn- cfg-items
  "Returns the list of prepared configuration items as specifies by the user."
  [cli-options]
  (-> (cli-opts/cli-args cli-options)
      (cfg-items/prepare (current-os/current-os))))

(defn- copy-file
  [cfg-file dst-file]
  (when (fs/exists? (fs/expand-home cfg-file))
    (if (fs/directory? (fs/expand-home cfg-file))
      (do (fs/create-dirs (-> dst-file
                              str))
          (fs/copy-tree (fs/expand-home cfg-file) dst-file))
      (do (fs/create-dirs (-> dst-file
                              fs/parent
                              str))
          (fs/copy (fs/expand-home cfg-file) dst-file)))))

(defn- parsed-cli-opts
  "Parse the `cli-opts` defined in this parameter and return it, except if an error occurs, so `(on-error-code err-code)` is called with `err-code` the exit code."
  [cli-args task-cli-opts on-error-code]
  (let [parsed-cli-opts (cli-opts/validate-task cli-args task-cli-opts)]
    (when (cli-opts/get parsed-cli-opts :verbose)
      (println "Verbose mode: ")
      (println "   Arguments: " (get-in parsed-cli-opts
                                        [:parsed-cli-opts :arguments]))
      (println "   Options: " (get-in parsed-cli-opts
                                      [:parsed-cli-opts :options])))
    (if-let [error-code (:error-code parsed-cli-opts)]
      (do (on-error-code error-code) nil)
      parsed-cli-opts)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-check
  "Call the check of cfg-items."
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt
                       cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps
        (-> (mapcat (fn [[_ {:keys [check-cmds]}]] check-cmds)
                    cfg-items)
            (ncmds/execute-all-cmds
             {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                 (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                 (cli-opts/get parsed-cli-opts :verbose)
                                 (println "Executing: " %)),
              :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
              :post-cmd-fn (fn [_ error-map]
                             (when-let [error-code (:error-code error-map)]
                               (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-clean
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt
                       cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps
        (-> (mapcat (fn [[_ {:keys [clean-cmds]}]] clean-cmds)
                    cfg-items)
            (ncmds/merge-cmds ["brew" "cleanup"]
                              []
                              (constantly true))
            (ncmds/execute-all-cmds
             {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                 (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                 (cli-opts/get parsed-cli-opts :verbose)
                                 (println "Executing: " %)),
              :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
              :post-cmd-fn (fn [_ error-map]
                             (when-let [error-code (:error-code error-map)]
                               (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-init
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt
                       cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps
        (-> (mapcat (fn [[_ {:keys [init-cmds]}]] init-cmds)
                    cfg-items)
            (ncmds/execute-all-cmds
             {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                 (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                 (cli-opts/get parsed-cli-opts :verbose)
                                 (println "Executing: " %)),
              :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
              :post-cmd-fn (fn [_ error-map]
                             (when-let [error-code (:error-code error-map)]
                               (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-install
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt
                       cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps
        (-> (mapcat (fn [[_ {:keys [install-cmds]}]] install-cmds)
                    cfg-items)
            ;;TODO merge- to work on,
            ;;TODO install should work in the right order
            (ncmds/merge-cmds ["brew" "cleanup"]
                              []
                              (constantly true))
            (ncmds/execute-all-cmds
             {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                 (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                 (cli-opts/get parsed-cli-opts :verbose)
                                 (println "Executing: " %)),
              :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
              :post-cmd-fn (fn [_ error-map]
                             (when-let [error-code (:error-code error-map)]
                               (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
;;TODO check
(defn ci-save
  [cli-args on-error-code]
  (fs/delete-tree backup-dir)
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts
                                         stop)
        cfg-items (cfg-items parsed-cli-opts)]
    (->> cfg-items
         (mapcat (fn [[cfg-item-name {:keys [cfg-files]}]]
                   (->> cfg-files
                        (map (fn [cfg-file] [cfg-file
                                             (clojure.core/format
                                              "%s/%s/%s"
                                              backup-dir
                                              (name cfg-item-name)
                                              (fs/file-name cfg-file))])))))
         (filterv some?))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-update
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts
                                         stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps (-> (mapcat (fn [[_ {:keys [update-cmds]}]] update-cmds)
                               cfg-items)
                       (ncmds/merge-cmds ["brew" "upgrade"]
                              []
                              (constantly true))
                       (ncmds/merge-cmds ["npm" "update" "-g"]
                              []
                              (constantly true))
                       (ncmds/execute-all-cmds
                        {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                            (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                            (cli-opts/get parsed-cli-opts :verbose)
                                            (println "Executing: " %)),
                         :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
                         :post-cmd-fn (fn [_ error-map]
                                        (when-let [error-code (:error-code error-map)]
                                          (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-version
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt
                       cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args
                                         task-cli-opts
                                         stop)
        cfg-items (cfg-items parsed-cli-opts)
        error-maps
        (-> (mapcat (fn [[_ {:keys [cfg-version-cmds]}]] cfg-version-cmds)
                    cfg-items)
            (ncmds/merge-cmds ["brew" "list" "--cask"]
                              ["--versions"]
                              (constantly true))
            (ncmds/merge-cmds ["brew" "list"]
                              ["--versions"]
                              #(not (contains? (set %) "--cask")))
            (ncmds/execute-all-cmds
             {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                 (println (clojure.core/format "-> : \"%s\"" (str/join " " %)))
                                 (cli-opts/get parsed-cli-opts :verbose)
                                 (println "Executing: " %)),
              :sandbox? (cli-opts/get parsed-cli-opts :sandbox)
              :post-cmd-fn (fn [_ error-map]
                             (when-let [error-code (:error-code error-map)]
                               (on-error-code error-code)))}))]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn format
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)
        error-maps
        (ncmds/execute-all-cmds
         [["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w" "{}"]]
         {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                             (println "Cmd : " %)
                             (cli-opts/get parsed-cli-opts :verbose)
                             (println "Executing: " %)),
          :post-cmd-fn (fn [_ error-map]
                         (when-let [error-code (:error-code error-map)]
                           (on-error-code error-code)))})]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn show
  [cli-args on-error-code]
  (let [task-cli-opts []]
    (when-let [parsed-cli-opts
               (parsed-cli-opts cli-args task-cli-opts stop)]
      (let [cfg-items (cfg-items parsed-cli-opts)]
        ;;TODO Check the ordering
        (when (cli-opts/get parsed-cli-opts :verbose)
          (println "List of cfg-items :")
          (pp/pprint cfg-items))
        (let [{:keys [cycle-detected subgraph-with-cycle sorted],
               :as cfg-items-by-layers}
              (cfg-items/cfg-items-by-layers cfg-items)]
          (if cycle-detected
            (do (println "Setup is invalid - a cycle has been detected.")
                (println "subgraph-with-cycle: " (keys subgraph-with-cycle))
                (println "sorted: " sorted))
            (run! println
                  (->> (interleave (range) (:sorted cfg-items-by-layers))
                       (partition 2)
                       (map (partial str/join " -> "))))))
        nil))))
