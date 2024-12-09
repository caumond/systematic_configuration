(ns core
  (:refer-clojure :exclude [update])
  (:require [babashka.fs :as fs]
            [current-os]
            [clojure.pprint :as pp]
            [cfg-items]
            [cfg-items.read]
            [cfg-items-schema]
            [ncmds]
            [cli-opts :as cli-opts :refer [parsed-cli-opts]]
            [clojure.string :as str]))

(def ^:private backup-dir "cfg_backup")

(defn- stop [n] (System/exit (or n 0)))

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
  ([cli-options] (cfg-items cli-options (current-os/current-os))))

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

(defn ci-check
  "Call the check of cfg-items."
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :check-cmds
            #(ncmds/merge-cmds % ["npm" "doctor"] [] (constantly true))))

(defn ci-clean
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :clean-cmds
            #(ncmds/merge-cmds % ["brew" "cleanup"] [] (constantly true))))

(defn ci-init
  [cli-args on-error-code]
  (run-cmds cli-args on-error-code :init-cmds identity))

(defn ci-install
  [cli-args on-error-code]
  (try (fs/delete-tree "tmp/")
       (catch Exception e
         (on-error-code (str "Error during the deletion of tmp directory.\n"
                             e))))
  (run-cmds cli-args on-error-code :install-cmds identity))

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

(defn ci-update
  [cli-args on-error-code]
  (run-cmds cli-args
            on-error-code
            :update-cmds
            #(->
               %
               (ncmds/merge-cmds ["brew" "upgrade"] [] (constantly true))
               (ncmds/merge-cmds ["npm" "update" "-g"] [] (constantly true)))))

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

(defn validate
  []
  (if-let [errors (-> cfg-items/cfg-filename
                      cfg-items.read/read-data-as-resource
                      cfg-items-schema/validate-cfg)]
    (do (println "Error in the configuration: ") (pp/pprint errors))
    (println "Is valid configuration file.")))
