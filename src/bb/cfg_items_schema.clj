(ns cfg-items-schema
  "What is the data schema for configuration items."
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]))

(def ^:private malli-registry (merge (m/default-schemas) (mu/schemas)))

(def cfg-item-name :keyword)

(def cmds [:sequential [:vector :string]])

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos {:subdir "macos"}, :ubuntu {:subdir "ubuntu"}})

(def os-name
  (->> (keys cfg-envs)
       (concat [:enum])
       vec))

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

;; ********************************************************************************
;; Public API

(def registry
  (assoc malli-registry
         ::cfg-item-per-os
         [:or cfg-item-per-os [:vector cfg-item-per-os]]))

(def cfg-items-schema
  "`cfg-items` is a map associating `cfg-item-name` to their `cfg-item-per-os`."
  [:map-of cfg-item-name [:ref ::cfg-item-per-os]])

(defn validate-cfg
  [file-content]
  (when-not (m/validate cfg-items-schema file-content {:registry registry})
    {:error (->> file-content
                 (m/explain (m/schema cfg-items-schema {:registry registry}))
                 me/with-spell-checking
                 me/humanize)}))
