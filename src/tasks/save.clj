(ns tasks.save
  (:require [babashka.fs :as fs]
            [cmds]
            [cfg-items]))

(def backup-dir "cfg_backup")

(defn save
  "Save
  Params:
  * `os` kewyord among `[:macos, :ubuntu]`"
  [os cfg-item sandbox?]
  (let [cfg-items (->> (cfg-items/read-configuration os cfg-item)
                       (filter (fn [[_ v]] (:cfgs v))))]
    (println (format "Save following tasks `%s`" (mapv first cfg-items)))
    (println "Clean all existing files in " backup-dir)
    (fs/delete-tree backup-dir)
    (doseq [cfg-item cfg-items]
      (when-let [[cfg-item-name {:keys [cfgs]}] cfg-item]
        (println (format "Save cfg `%s`" cfg-item-name))
        (doseq [cfg cfgs]
          (let [dst-file (format "%s/%s/%s"
                                 backup-dir
                                 (name cfg-item-name)
                                 (fs/file-name cfg))]
            (println (format "Copy file `%s` to `%s`" cfg dst-file))
            (when-not sandbox?
              (if (fs/exists? (fs/expand-home cfg))
                (if (fs/directory? (fs/expand-home cfg))
                  (do (fs/create-dirs (-> dst-file
                                          str))
                      (fs/copy-tree (fs/expand-home cfg) dst-file))
                  (do (fs/create-dirs (-> dst-file
                                          fs/parent
                                          str))
                      (fs/copy (fs/expand-home cfg) dst-file)))
                (println (format
                          "File copy of `%s` is skipped, as it doesn't exist"
                          cfg))))))))))

(comment
  (save :macos nil false)
  ;
)
