(ns utils
  (:require [clojure.string :as str]))

(defn deep-merge
  "Deep merge nested maps.
  Last map has higher priority

  This code comes from this [gist](https://gist.github.com/danielpcox/c70a8aa2c36766200a95)"
  [& maps]
  (apply merge-with
    (fn [& args]
      (if (every? #(or (map? %) (nil? %)) args)
        (apply deep-merge args)
        (last args)))
    maps))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- brew-string-to-config
  []
  (->>
    (str/split
      "brew install git zsh ripgrep fd gnupg nano tree rlwrap htop cmake jq graphviz shellcheck node openjdk androidstudio clojure/tools/clojure coreutils grep clojure-lsp/brew/clojure-lsp-native borkdude/brew/clj-kondo tidy-html5 python markdown wget ffmpeg vlc shfmt ktlint"
      #" ")
    (mapv (fn [v] [(keyword v) {:formula v, :type :brew}]))
    (into {})))

(defn- pip-string-to-config
  []
  (->> (str/split
         "black isort pipenv testresources nose virtualenv pytest pyflakes"
         #" ")
       (mapv (fn [v] [(keyword v) {:package v, :type :pip3}]))
       (into {})))

(comment
  (brew-string-to-config)
  (pip-string-to-config)
  ;
)
