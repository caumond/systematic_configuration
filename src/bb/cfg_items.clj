(ns cfg-items
  "`cfg-items` stands for configuration items.

  They are describing one element to be installed, whatever the OS."
  (:require [dag]
            [dag.map]
            [cfg-items.read]
            [cfg-items.cmds :refer [expand-package-managers]]))

(def cfg-filename "Name of the configuration file to load" "cfg_item.edn")

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
;;TODO And the `pre-reqs` that are not active for this os should be removed
;;from pre-reqs, so bb show -o ubuntu

(defn cfg-items-by-layers
  [cfg-items]
  (dag/topological-layers cfg-items (dag.map/simple :cfg-item-deps) 10))

(defn build
  "Build a configuration items for `os` and limited to the names in `cfg-item-names`.
  If `cfg-item-names` is empty, all elements are returned."
  [cfg-item-names os]
  (-> (cfg-items.read/read-data-as-resource cfg-filename)
      (normalize os cfg-item-names)
      expand-package-managers))

;; ********************************************************************************
;; Public API

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
