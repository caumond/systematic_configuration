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

(def ^:private separator (apply str (conj (vec (repeat 10 "*")) "\n")))

(def sandbox-cli-opt
  ["-s" "--sandbox"
   "In sandbox no command is executed so configuration is not modified, just what would be done is."])

(def ^:private cmd-execution-cli-opt
  ["-e" "--exception" "See details of the exception."])

(defn- cfg-items
  "Returns the list of prepared configuration items as specifies by the user."
  ([cli-options os]
   (-> (cli-opts/cli-args cli-options)
       (cfg-items/build os)))
  ([cli-options]
   (cfg-items cli-options (current-os/current-os))))

(defn- copy-file
  [cfg-file dst-file]
  (let [cfg-file (fs/expand-home cfg-file)
        dst-file (fs/expand-home dst-file)]
    (when (fs/exists? cfg-file)
      (if (fs/directory? cfg-file)
        (do (fs/create-dirs (fs/expand-home (str dst-file)))
            (fs/copy-tree cfg-file
                          dst-file
                          {:replace-existing true,
                           :nofollow-links false,
                           :copy-attributes true}))
        (do (fs/create-dirs (fs/expand-home (str (fs/parent dst-file))))
            (fs/copy cfg-file
                     dst-file
                     {:replace-existing true,
                      :nofollow-links false,
                      :copy-attributes true}))))))

(defn- parsed-cli-opts
  "Parse the `cli-opts` defined in this parameter and return it, except if an error occurs, so `(on-error-code err-code)` is called with `err-code` the exit code."
  [cli-args task-cli-opts on-error-code]
  (let [parsed-cli-opts (cli-opts/validate-task cli-args task-cli-opts)]
    (when (cli-opts/get parsed-cli-opts :verbose)
      (println "Verbose mode: ")
      (println "   Arguments: "
               (get-in parsed-cli-opts [:parsed-cli-opts :arguments]))
      (println "   Options: "
               (get-in parsed-cli-opts [:parsed-cli-opts :options])))
    (if-let [error-code (:error-code parsed-cli-opts)]
      (do (on-error-code error-code) nil)
      parsed-cli-opts)))

(defn- run-cmds
  [cli-args on-error-code cmd-kw merging-fn]
  (try
    (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
          parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)
          cfg-items (cfg-items parsed-cli-opts)
          ordered-cfg-items (->> cfg-items
                                 cfg-items/cfg-items-by-layers
                                 (cfg-items/ordered-cfg-items cfg-items)
                                 (map vec))
          error-maps
          (-> (mapcat (fn [[_ check-cmd]] (get check-cmd cmd-kw))
                      ordered-cfg-items)
              merging-fn
              (ncmds/execute-all-cmds
               (cond-> {:pre-cmd-fn
                        #(cond (cli-opts/get parsed-cli-opts :sandbox)
                               (println (clojure.core/format "-> : \"%s\""
                                                             (str/join " " %)))
                               (cli-opts/get parsed-cli-opts :verbose)
                               (println "Executing: " %)),
                        :sandbox? (cli-opts/get parsed-cli-opts :sandbox),
                        :post-cmd-fn
                        (fn [_ error-map]
                          (when-let [error-code (:error-code error-map)]
                            (on-error-code "Post command execution failed: "
                                           error-code)))}
                 (:exception parsed-cli-opts)
                 (assoc :exception (:exception parsed-cli-opts)))))]
      (ncmds/println-summary-errors error-maps
                                    (cli-opts/get parsed-cli-opts :exception)))
    (catch Exception e
      (when (:exception parsed-cli-opts)
        (on-error-code (str "Exception: \n" (with-out-str (pp/pprint e))) 4)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-check
  "Call the check of cfg-items."
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :check-cmds
            #(ncmds/merge-cmds % ["npm" "doctor"] [] (constantly true))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-clean
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :clean-cmds
            #(ncmds/merge-cmds % ["brew" "cleanup"] [] (constantly true))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-init
  [cli-args on-error-code]
  (run-cmds cli-args on-error-code :init-cmds identity))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-install
  [cli-args on-error-code]
  (try (fs/delete-tree "tmp/")
       (catch Exception e
         (on-error-code (str "Error during the deletion of tmp directory.\n"
                             e))))
  (run-cmds cli-args on-error-code :install-cmds identity))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-save
  [cli-args on-error-code]
  (try (fs/delete-tree backup-dir)
       (catch Exception e
         (on-error-code (str (clojure.core/format
                              "Error during deletion of directory %s"
                              backup-dir)
                             e)
                        4)))
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        verbose? (cli-opts/get parsed-cli-opts :verbose)
        sandbox? (cli-opts/get parsed-cli-opts :sandbox)
        files (->> cfg-items
                   (mapcat (fn [[cfg-item-name {:keys [cfg-files]}]]
                             (->> cfg-files
                                  (map (fn [cfg-file] [cfg-file
                                                       (clojure.core/format
                                                        "%s/%s/%s"
                                                        backup-dir
                                                        (name cfg-item-name)
                                                        (fs/file-name
                                                         cfg-file))])))))
                   (filterv some?))]
    (run! (fn [[src dst]]
            (when (or sandbox? verbose?) (println src "->" dst))
            (try (when-not sandbox? (copy-file src dst))
                 (catch Exception e
                   (on-error-code (str (clojure.core/format
                                        "Error during copy of `%s` to `%s`"
                                        src
                                        dst)
                                       e)
                                  9))))
          files)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-restore
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)
        cfg-items (cfg-items parsed-cli-opts)
        verbose? (cli-opts/get parsed-cli-opts :verbose)
        sandbox? (cli-opts/get parsed-cli-opts :sandbox)
        files (->> cfg-items
                   (mapcat (fn [[cfg-item-name {:keys [cfg-files]}]]
                             (->> cfg-files
                                  (map (fn [cfg-file] [cfg-file
                                                       (clojure.core/format
                                                        "%s/%s/%s"
                                                        backup-dir
                                                        (name cfg-item-name)
                                                        (fs/file-name
                                                         cfg-file))])))))
                   (filterv some?))]
    (run! (fn [[dst src]]
            (when (or sandbox? verbose?) (println src "->" dst))
            (try (when-not sandbox? (copy-file src dst))
                 (catch Exception e
                   (on-error-code (str (clojure.core/format
                                        "Error during copy of `%s` to `%s`"
                                        src
                                        dst)
                                       e)
                                  3))))
          files)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-update
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :update-cmds
            #(->
              %
              (ncmds/merge-cmds ["brew" "upgrade"] [] (constantly true))
              (ncmds/merge-cmds ["npm" "update" "-g"] [] (constantly true)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ci-version
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :cfg-version-cmds
            (fn [cfg-versions]
              (-> cfg-versions
                  (ncmds/merge-cmds ["brew" "list" "--cask"]
                                    ["--versions"]
                                    (constantly true))
                  (ncmds/merge-cmds ["brew" "list"]
                                    ["--versions"]
                                    #(not (contains? (set %) "--cask")))))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn format
  [cli-args on-error-code]
  (let [task-cli-opts [sandbox-cli-opt cmd-execution-cli-opt]
        parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)
        error-maps (ncmds/execute-all-cmds
                    [["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w"
                      "{}"]]
                    {:pre-cmd-fn #(cond (cli-opts/get parsed-cli-opts :sandbox)
                                        (println "Cmd : " %)
                                        (cli-opts/get parsed-cli-opts :verbose)
                                        (println "Executing: " %)),
                     :sandbox? (cli-opts/get parsed-cli-opts :sandbox),
                     :post-cmd-fn
                     (fn [cmd error-map]
                       (when-let [error-code (:error-code error-map)]
                         (on-error-code (format "Command `%s` with error " cmd)
                                        error-code)))})]
    (ncmds/println-summary-errors error-maps
                                  (cli-opts/get parsed-cli-opts :exception))))

(def os-cli-par
  ["-o" "--os OS"
   "In sandbox no command is executed so configuration is not modified, just what would be done is."
   :validate [string? "Must be a string."]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn show
  [cli-args]
  (let [task-cli-opts [os-cli-par]]
    (when-let [parsed-cli-opts (parsed-cli-opts cli-args task-cli-opts stop)]
      (let [os-par (cli-opts/get parsed-cli-opts :os)
            cfg-items (if (nil? os-par)
                        (cfg-items parsed-cli-opts)
                        (cfg-items parsed-cli-opts os-par))
            {:keys [cycle-detected subgraph-with-cycle sorted],
             :as cfg-items-by-layers}
            (cfg-items/cfg-items-by-layers cfg-items)]
      ;;  (pp/pprint cfg-items)
        (when (cli-opts/get parsed-cli-opts :verbose)
          (println "Sorted:")
          (pp/pprint (cfg-items/ordered-cfg-items
                      cfg-items
                      cfg-items-by-layers)))
        (when cycle-detected
          (do (println "Setup is invalid - a cycle has been detected.")
              (println "subgraph-with-cycle: " (keys subgraph-with-cycle))))
        (run! println
              (->> (interleave (range) sorted)
                   (partition 2)
                   (map (partial str/join " -> "))))
        nil))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn validate
  []
  (if-let [errors (-> cfg-items/cfg-filename
                      cfg-items/read-data-as-resource
                      cfg-items/validate-cfg)]
    (do (println "Error in the configuration: ") (pp/pprint errors))
    (println "Is valid configuration file.")))
