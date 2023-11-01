(ns cfg-items
  "Configuration items management"
  (:require [utils]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn- pip-update-cfg-item
  [{:keys [package], :as cfg-item-val}]
  (assoc cfg-item-val
         :install [["pip3" "install" package]]
         :update [["pip3" "install" "--upgrade" package]]
         :check [["pip3" "check" package]]))

(defn- brew-update-cfg-item
  [{:keys [tap formula], :as cfg-item-val}]
  (assoc cfg-item-val
         :install (concat (when tap [["brew" "tap" tap]])
                          [["brew" "install" formula]])
         :update [["brew" "upgrade" formula]]))

(def ^:private type-to-update-fn
  "Map the type of the modification, as seen in the configuration files and map the function"
  {:brew brew-update-cfg-item
   :pip3 pip-update-cfg-item})

(defn- process-types
  "For each predefined type"
  [configurations]
  (mapv (fn [[cfg-item val]]
          (if-let [update-fn (get type-to-update-fn (:type val))]
            [cfg-item (update-fn val)]
            [cfg-item val]))
        configurations))

(defn- read-data-as-resource
  [filename]
  (try (->> filename
            io/resource
            slurp
            edn/read-string)
       (catch Exception _
         (println (format "File `%s` could not be loaded" filename))
         nil)))

(def ^:private cfg-dir "Where configuration per os are stored" "os")

(def ^:private cfg-envs
  "Name of each os subdir"
  {:macos "macos", :ubuntu "ubuntu"})

(defn read-configuration
  "Read the merged configuration of what is necessary and how it is done for each os
  Params:
  * `os` keyword among (:macos, :ubuntu)"
  [os cfg-item]
  (println "configuration item" cfg-item)
  (let [configurations (utils/deep-merge (read-data-as-resource "cfg_item.edn")
                                         (->> os
                                              cfg-envs
                                              (format "%s/%s.edn" cfg-dir)
                                              read-data-as-resource))
        configurations (if (nil? cfg-item)
                         configurations
                         (select-keys configurations [cfg-item]))]
    (process-types configurations)))

(comment
  (read-configuration :macos nil)
  ;
  )
