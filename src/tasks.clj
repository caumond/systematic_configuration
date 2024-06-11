(ns tasks
  (:refer-clojure :exclude [format update])
  (:require [babashka.fs :as fs]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn cfg-version
  [cfg-items]
  (->> cfg-items
       (map (fn [[_ {:keys [cfg-version-cmd]}]] cfg-version-cmd))
       (filterv some?)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn check
  "Call the check of cfg-items."
  [cfg-items]
  (->> cfg-items
       (map (fn [[_ {:keys [check-cmd]}]] check-cmd))
       (filterv some?)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn clean
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[_ {:keys [clean-cmds]}]] clean-cmds))
       (filterv some?)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn format
  "Format clojure Files."
  []
  ["fd" "." "-tf" "-e" "clj" "-e" "edn" "-x" "zprint" "-w" "{}"])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[_ {:keys [init-cmds]}]] init-cmds))
       (filterv some?)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn install
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[_ {:keys [install-cmds]}]] install-cmds))))

(def backup-dir "cfg_backup")

(defn save
  "Save laptop configuration in the repo.
  `os` is a kewyord among `[:macos, :ubuntu]` telling which configuration type to save."
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[cfg-item-name {:keys [cfg-files]}]]
                 (->> cfg-files
                      (map (fn [cfg-file] [cfg-file
                                           (clojure.core/format
                                            "%s/%s/%s"
                                            backup-dir
                                            (name cfg-item-name)
                                            (fs/file-name cfg-file))])))))
       (filterv some?)))

(defn copy-file
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

(defn update
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[_ {:keys [update-cmds]}]] update-cmds))
       (filterv some?)))

(defn validate
  [cfg-items]
  (->> cfg-items
       (mapcat (fn [[_ {:keys [validate-cmds]}]] validate-cmds))))
