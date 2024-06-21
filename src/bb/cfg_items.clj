(ns cfg-items
  "`cfg-items` stands for configuration items."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [dag]
            [dag.map]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            [utils]))

(def ^:private malli-registry (merge (m/default-schemas) (mu/schemas)))

(def ^:private cfg-dir "Directory where configuration per os are stored" "os")

(def cfg-filename "cfg_item.edn")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos {:subdir "macos"}, :ubuntu {:subdir "ubuntu"}})

(def os-name
  (->> (keys cfg-envs)
       (concat [:enum])
       vec))

(def cmds [:sequential [:vector :string]])

(def cfg-item-name :keyword)

(def brew-package-manager
  "Package management with brew."
  [:map {:closed true} [:package-manager [:enum :brew]]
   [:cask {:description "Is the formula a cask?", :optional true} :boolean]
   [:formula {:description "`brew` formula to install the `cfg-item`."} :string]
   [:install-options
    {:optional true,
     :description "What options to be added when installing the formula."}
    [:vector :string]]
   [:tap {:optional true, :description "`brew` tap where to find the formula."}
    :string]])

(def npm-package-manager
  "Package management with npm."
  [:map {:closed true} [:package-manager [:enum :npm]]
   [:npm-deps {:description "`npm` dependency."} [:vector :string]]])

(def manual-package-manager
  "No package management, done manually with sh commands."
  [:map {:closed true} [:package-manager [:enum :manual]]
   [:check-cmds
    {:optional true, :description "Check the installation of the `cfg-item`."}
    cmds]
   [:clean-cmds {:optional true, :description "Clean the `cfg-item`."} cmds]
   [:init-cmds
    {:optional true,
     :description "Commands to init - run once - the configuration item."} cmds]
   [:install-cmds
    {:description "Commands describing how to manually install the `cfg-item`.",
     :optional true} cmds]
   [:update-cmds
    {:optional true, :description "Update the installation of `cfg-item`."}
    cmds]
   [:cfg-version-cmds
    {:optional true, :description "Command for displaying the version."} cmds]])

(def common-behavior
  [:map
   [:description
    {:optional true, :description "Optional description of the `cfg-item`."}
    :string]
   [:post-package
    {:optional true,
     :description "Command to setup after package has been installed."} cmds]
   [:deps
    {:optional true,
     :description
     "List of `cfg-item-name`s that this configuration item depends on."}
    [:sequential cfg-item-name]]
   [:pre-reqs
    {:optional true,
     :description
     "List of cfg-item pre requisites. They have the exact same compatibility."}
    [:map-of cfg-item-name [:ref ::cfg-item-per-os]]]
   [:cfg-files
    {:optional true,
     :description "Configuration files of this `cfg-item` to save."}
    [:vector :string]]
   [:tmp-files {:optional true, :description "Temporary files to remove."}
    [:vector :string]] [:os {:optional true} os-name]
   [:tmp-dirs {:optional true, :description "Temporary directory to remove."}
    [:vector :string]]])

(def cfg-item-per-os
  [:or [:union npm-package-manager common-behavior]
   [:union manual-package-manager common-behavior]
   [:union brew-package-manager common-behavior]])

(def registry
  (assoc malli-registry
         ::cfg-item-per-os
         [:or cfg-item-per-os [:vector cfg-item-per-os]]))

(def cfg-items-schema
  "`cfg-items` is a map associating `cfg-item-name` to their `cfg-item-per-os`."
  [:map-of cfg-item-name [:ref ::cfg-item-per-os]])

(defn brew-update
  "Create commands for a brew package manager."
  [{:keys [tap formula package-manager cask install-options], :as _cfg-item}]
  (when (= package-manager :brew)
    {:cfg-version-cmds [(vec (concat ["brew" "list"]
                                     (when cask ["--cask"])
                                     [formula "--versions"]))],
     :check-cmds [], ;; brew cfg-item is checking all managed cfg-items at
     ;; once.
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

(defn npm-update
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

(defn manual-update
  "Create commands for the manual package manager."
  [{:keys [package-manager], :as cfg-item}]
  (when (= package-manager :manual)
    (select-keys cfg-item
                 [:cfg-version-cmds :check-cmds :clean-cmds :init-cmds
                  :install-cmds :update-cmds])))

(defn common-update
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

(defn expand-package-managers
  [cfg-items]
  (->> cfg-items
       (mapv (fn [[cfg-item-name cfg-item]]
               [cfg-item-name
                (->> ((juxt brew-update npm-update manual-update common-update)
                      cfg-item)
                     (apply merge))]))
       (into {})))

(defn read-data-as-resource
  [filename]
  (try (->> filename
            io/resource
            slurp
            edn/read-string)
       (catch Exception _
         (println (format "File `%s` could not be loaded" filename))
         nil)))

(defn filter-cfg-item-names
  "Filter `cfg-items` to the one declared in `cfg-item-names`. If `cfg-item-names` is `nil`, none is removed."
  [cfg-items cfg-item-names]
  (cond-> cfg-items (seq cfg-item-names) (select-keys cfg-item-names)))

(defn limit-to-os
  "Return the `cfg-item` concerning the os matching `oss`."
  [oss cfg-item-per-os]
  (->> cfg-item-per-os
       (filter #(contains? oss (:os %)))
       first
       vector
       (into {})))

(defn set-os
  [cfg-item-per-os os-to-set]
  (let [os-to-set (if (nil? os-to-set) :all os-to-set)]
    (->> cfg-item-per-os
         (mapv (fn [{:keys [os], :as cfg-item}]
                 (assoc cfg-item
                        :os
                        (cond (not= :all os-to-set) os-to-set
                              (and (= :all os-to-set) (some? os) (not= :all os))
                              os
                              (and (= :all os-to-set) (nil? os)) :all
                              :else os)))))))

(defn set-cfg-items-os
  [cfg-items os-to-set]
  (-> cfg-items
      (update-vals
       (fn [cfg-item-per-os]
         (-> (if (map? cfg-item-per-os) [cfg-item-per-os] cfg-item-per-os)
             (set-os os-to-set))))))

(defn extract-per-reqs
  "Extract `pre-reqs` in the `cfg-items`.

  Returns a map with two entries:
  `:develop` with  `cfg-item` which `pre-reqs` is removed, replaced with `deps` containing its keys only.
  `:to-develop` containing the element formerly in `pre-reqs`."
  [{:keys [pre-reqs os], :as cfg-item-with-one-os}]
  (cond-> {:developped (dissoc cfg-item-with-one-os :pre-reqs)}
    (some? pre-reqs) (assoc :to-develop (set-cfg-items-os pre-reqs os))
    (map? pre-reqs)
    (update-in [:developped :deps] (comp set concat) (keys pre-reqs))))

(defn normalize
  "Turns `cfg-items` into a normal form matching:
  * only the values matching `os`,
  * limited to `cfg-item-names`,
  * all expanded to root."
  [cfg-items os cfg-item-names]
  (loop [to-normalize cfg-items
         ncfg-items {}
         max-loops 100]
    (let [[cfg-item-name cfg-item-os] (first to-normalize)
          cfg-item-with-one-os
          (->> (if (vector? cfg-item-os) cfg-item-os [cfg-item-os])
               (limit-to-os #{:all nil os}))
          {:keys [developped to-develop]} (extract-per-reqs
                                           cfg-item-with-one-os)
          ncfg-items (cond-> ncfg-items
                       (seq developped) (assoc cfg-item-name developped))
          to-normalize (concat (rest to-normalize) to-develop)]
      (if (and (seq to-normalize) (pos? max-loops))
        (recur to-normalize ncfg-items (dec max-loops))
        ncfg-items))))

;;TODO For a reason I don't get, os is not pushed in show
;;TODO And the `pre-reqs` that are not active for this os should be removed from pre-reqs, so bb show -o ubuntu
(defn validate-cfg
  [file-content]
  (when-not (m/validate cfg-items-schema file-content {:registry registry})
    {:error (->> file-content
                 (m/explain (m/schema cfg-items-schema {:registry registry}))
                 me/with-spell-checking
                 me/humanize)}))

(defn cfg-items-by-layers
  [cfg-items]
  (dag/topological-layers cfg-items (dag.map/simple :cfg-item-deps) 10))

(defn build
  "Build a configuration items for `os` and limited to the names in `cfg-item-names`.
  If `cfg-item-names` is empty, all elements are returned."
  [cfg-item-names os]
  (-> (read-data-as-resource cfg-filename)
      (normalize os cfg-item-names)
      expand-package-managers))

(defn ordered-cfg-items
  [cfg-items cfg-items-by-layers]
  (->> cfg-items-by-layers
       :sorted
       (apply concat)
       (mapcat #(vector % (get cfg-items %)))
       (apply array-map)))

;; Seems to fail as already installed
;; [tmp/brew/brew-install.sh]
;; [tmp/ohmyzsh-install.sh]
;; [git clone https://github.com/BurntSushi/ripgrep tmp/rg]
;; [cargo -C tmp/rg build --release]
;; [git clone --depth 1 https://github.com/doomemacs/doomemacs ~/.config/emacs]

;; What's up?
;; [brew reinstall zprint -q]
;; [brew reinstall dockutil -q]
;; [brew reinstall clever-tools -q]
;; [brew reinstall emacs-plus@29 -q --with-imagemagick --with-native-comp]
