(ns cfg-items.cmds "Build the commands of configuration items")

(defn brew
  "Create commands for a brew package manager."
  [{:keys [tap formula package-manager cask install-options], :as _cfg-item}]
  (when (= package-manager :brew)
    {:cfg-version-cmds [(vec (concat ["brew" "list"]
                                     (when cask ["--cask"])
                                     [formula "--versions"]))],
     ;; brew cfg-item is checking all managed cfg-items at once.
     :check-cmds [],
     :clean-cmds [["brew" "cleanup" formula]],
     :cfg-item-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds (->> [(vec (concat ["brew" "reinstall"]
                                      (when cask ["--cask"])
                                      [formula "-q"]
                                      (when install-options install-options)))]
                        (concat (when tap [["brew" "tap" tap]]))
                        vec),
     :update-cmds [["brew" "upgrade" formula]]}))

(defn npm
  "Create commands for an npm package manager."
  [{:keys [npm-deps package-manager], :as _cfg-item}]
  (when (= package-manager :npm)
    {:cfg-version-cmds [],
     :check-cmds (mapv (fn [npm-dep] ["npm" "doctor" npm-dep]) npm-deps),
     :clean-cmds [], ;; npm cache clean is discouraged by npm.
     :cfg-item-deps [package-manager],
     :init-cmds [], ;; no need
     :install-cmds (mapv (fn [npm-dep] ["npm" "install" "-g" npm-dep])
                         npm-deps),
     :update-cmds (mapv (fn [npm-dep] ["npm" "update" "-g" npm-dep])
                        npm-deps)}))

(defn manual
  "Create commands for the manual package manager."
  [{:keys [package-manager], :as cfg-item}]
  (when (= package-manager :manual)
    (select-keys cfg-item
                 [:cfg-version-cmds :check-cmds :clean-cmds :init-cmds
                  :install-cmds :update-cmds])))

(defn common
  "Create common commands for the package manager."
  [{:keys [tmp-files clean-cmds pre-reqs deps tmp-dirs post-package cfg-files],
    :as _cfg-item}]
  (cond-> {}
    (seq clean-cmds) (assoc :clean-cmds (vec clean-cmds))
    (seq tmp-files) (update :clean-cmds
                            (comp vec concat)
                            (->> tmp-files
                                 (map (fn [tmp-file] ["rm" "-f" tmp-file]))))
    (seq tmp-dirs) (update :clean-cmds
                           (comp vec concat)
                           (->> tmp-dirs
                                (map (fn [tmp-dir] ["rm" "-fr" tmp-dir]))))
    cfg-files (assoc :cfg-files (vec cfg-files))
    post-package (assoc :post-package post-package)
    pre-reqs (update :cfg-item-deps (comp vec concat) (keys pre-reqs))
    deps (update :cfg-item-deps (comp vec dedupe sort vec concat) deps)))

;; ********************************************************************************
;; Public

(defn expand-package-managers
  [cfg-items]
  (-> cfg-items
      (update-vals (fn [cfg-item]
                     (->> ((juxt brew npm manual common) cfg-item)
                          (apply merge))))))
