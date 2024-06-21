(ns cfg-items-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [malli.error :as me]
            [cfg-items :as sut]
            [ncmds]))

(defn humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(deftest cfg-item-schema-test
  (is (m/schema sut/cfg-items-schema {:registry sut/registry})
      "Raise an exception if schema is invalid.")
  (is (= nil
         (-> (m/schema sut/cfg-items-schema {:registry sut/registry})
             (humanize {})))))

(deftest brew-update-test
  (testing "Formula without tap."
    (is (= nil
           (humanize sut/brew-package-manager
                     {:formula "aspell", :package-manager :brew}))
        "The schema of only one formula is accepted.")
    (is (some? (:package-manager (humanize sut/brew-package-manager
                                           {:formula "aspell",
                                            :package-manager :brow})))
        "Wrong package manager is rejected.")
    (is (= {:cfg-version-cmds [["brew" "list" "aspell" "--versions"]],
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :cfg-item-deps [:brew],
            :init-cmds [],
            :install-cmds [["brew" "reinstall" "aspell" "-q"]],
            :update-cmds [["brew" "upgrade" "aspell"]]}
           (sut/brew-update {:formula "aspell", :package-manager :brew}))
        "Normal use case"))
  (testing "Formula with a tap."
    (is (= nil
           (humanize sut/brew-package-manager
                     {:package-manager :brew,
                      :formula "aspell",
                      :tap "d12frosted/emacs-plus"}))
        "The schema of formula and tap is accepted.")
    (is (= {:cfg-version-cmds [["brew" "list" "aspell" "--versions"]],
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :cfg-item-deps [:brew],
            :init-cmds [],
            :install-cmds [["brew" "tap" "d12frosted/emacs-plus"]
                           ["brew" "reinstall" "aspell" "-q"]],
            :update-cmds [["brew" "upgrade" "aspell"]]}
           (sut/brew-update {:package-manager :brew,
                             :formula "aspell",
                             :tap "d12frosted/emacs-plus"}))
        "Valid formula return expected commands.")))

(deftest npm-update-test
  (testing "With npm-dep only."
    (is (= nil
           (humanize sut/npm-package-manager
                     {:package-manager :npm, :npm-deps ["typewritten"]}))
        "the schema of formula and tap is accepted.")
    (is (some? (:package-manager (humanize sut/npm-package-manager
                                           {:package-manager :npm-old,
                                            :npm-deps ["typewritten"]})))
        "Wrong package manager is rejected")
    (is (= {:cfg-version-cmds [],
            :cfg-item-deps [:npm],
            :check-cmds [["npm" "doctor" "typewritten"]],
            :clean-cmds [],
            :init-cmds [],
            :install-cmds [["npm" "install" "-g" "typewritten"]],
            :update-cmds [["npm" "update" "-g" "typewritten"]]}
           (sut/npm-update {:npm-deps ["typewritten"], :package-manager :npm}))
        "Valid formula return expected commands.")))

(deftest manual-update-test
  (is (= nil
         (humanize sut/manual-package-manager
                   {:package-manager :manual, :install-cmds [["pwd"]]}))
      "Valid manual package.")
  (is (some? (:package-manager (humanize sut/manual-package-manager
                                         {:package-manager :manuel,
                                          :install-cmds [["pwd"]]})))
      "Invalid package manager is rejected")
  (is
   (= {:cfg-version-cmds ["ls" "-la"]}
      (sut/manual-update {:package-manager :manual,
                          :cfg-version-cmds ["ls" "-la"]}))
   "A manual package is cleaned from post-package, deps, pre-reqs, cfg-files tmp-files tmp-dirs and package-manager"))

(deftest common-update-test
  (is (= {} (sut/common-update {}))
      "It is possible to have no common parameter.")
  (is (= {}
         (sut/common-update {:clean-cmds []})
         (sut/common-update {:tmp-files [], :clean-cmds []})
         (sut/common-update {:tmp-dirs [], :clean-cmds []}))
      "No files deletions is ok.")
  (is (= {:clean-cmds [["clean" "clean"]]}
         (sut/common-update {:clean-cmds [["clean" "clean"]]}))
      "Clean commands are copied.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-f" "a"] ["rm" "-f" "b"]]}
         (sut/common-update {:tmp-files ["a" "b"],
                             :clean-cmds [["clean" "clean"]]}))
      "File deletion and clean commands deletion are merged.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-fr" "a"] ["rm" "-fr" "b"]]}
         (sut/common-update {:tmp-dirs ["a" "b"],
                             :clean-cmds [["clean" "clean"]]}))
      "File deletion and clean commands deletion are merged.")
  (is (= {:cfg-files ["a" "b"]} (sut/common-update {:cfg-files ["a" "b"]}))
      "Configuration files are copied")
  (is (= {:post-package {:a 1, :b 1}}
         (sut/common-update {:post-package {:a 1, :b 1}}))
      "Post package are copied")
  (is (= {:cfg-item-deps [:a :b]} (sut/common-update {:pre-reqs {:a 1, :b 1}}))
      "Pre reqs creates dependencies")
  (is (= {:cfg-item-deps [:a :b]} (sut/common-update {:deps [:a :b]}))
      "Deps are copied in dependencies")
  (is (= {:cfg-item-deps [:a :b :c :d]}
         (sut/common-update {:deps [:a :b], :pre-reqs {:c 2, :d 2, :b 1}}))
      "Deps and pre reqs are concatened")
  (is (= {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]]}
         (sut/common-update {:tmp-files ["a" "b" "cd"]})))
  (is (= {:clean-cmds [["rm" "-fr" "a"] ["rm" "-fr" "b"] ["rm" "-fr" "cd"]]}
         (sut/common-update {:tmp-dirs ["a" "b" "cd"]}))))

(deftest expand-package-managers-test
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                              ["rm" "-f" "cd"]]}}
         (sut/expand-package-managers {:test {:tmp-files ["a" "b" "cd"]}})))
  (is (=
       {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]]},
        :test2 {:cfg-version-cmds [["brew" "list" "black" "--versions"]],
                :check-cmds [],
                :clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                :cfg-item-deps [:brew],
                :init-cmds [],
                :install-cmds [["brew" "reinstall" "black" "-q"]],
                :update-cmds [["brew" "upgrade" "black"]]}}
       (sut/expand-package-managers {:test {:tmp-files ["a" "b" "cd"]},
                                     :test2 {:tmp-files ["a" "b" "cd"],
                                             :package-manager :brew,
                                             :formula "black"}}))))

(deftest filter-cfg-item-names-test
  (is (= 2
         (count (-> {:docker {}, :zprint {}}
                    (sut/filter-cfg-item-names nil)))
         (count (-> {:docker {}, :zprint {}}
                    (sut/filter-cfg-item-names []))))
      "Empty `cfg-item-names` list returns the whole map")
  (is (= 1
         (count (-> {:docker {}}
                    (sut/filter-cfg-item-names [:docker]))))
      "Limit configurations to existing one is ok.")
  (is (= {}
         (-> {:docker {:clean-cmds [["rm" "-fr" "tmp"]]}}
             (sut/filter-cfg-item-names [:doom])))
      "If not found, it returns empty maps."))

(deftest limit-to-os-test
  (is
   (= {:cask true,
       :formula "android-studio",
       :package-manager :brew,
       :os :macos}
      (sut/limit-to-os
       #{:macos}
       [{:cask true,
         :formula "android-studio",
         :package-manager :brew,
         :os :macos}
        {:formula "android-studio", :package-manager :brew, :os :unix}]))
   "When many configuration exists, only the one matching the current is chosen.")
  (is (empty? (sut/limit-to-os #{:unix}
                               [{:cask true,
                                 :formula "android-studio",
                                 :package-manager :brew,
                                 :os :macos}]))
      "If no configuration match the os, it is skipped")
  (is (= {:cask true, :formula "android-studio", :package-manager :brew}
         (sut/limit-to-os
          #{:macos nil}
          [{:cask true, :formula "android-studio", :package-manager :brew}]))
      "If one only setup exists it is avialable for all os."))

(deftest set-os-test
  (is (= [{:formula "zprint", :os :macos, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :os :macos, :package-manager :brew}]
                     :all))
      "Setting :all as an os is not modifying specified os")
  (is (= [{:formula "zprint", :os :all, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :package-manager :brew}] nil)
         (sut/set-os [{:formula "zprint", :os :all, :package-manager :brew}]
                     nil))
      "Setting `nil` as an os is like setting `:all`.")
  (is (= [{:formula "zprint", :os :all, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :package-manager :brew}] :all)
         (sut/set-os [{:formula "zprint", :os :all, :package-manager :brew}]
                     :all))
      "Setting `:all` as an os is defaulting `os` to `:all`.")
  (is (= [{:formula "zprint", :os :ubuntu, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :os :ubuntu, :package-manager :brew}]
                     :all))
      "`:all` does not superseed existing values")
  (is (= [{:formula "zprint", :os :ubuntu, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :os :ubuntu, :package-manager :brew}]
                     :macos))
      "`:macos` does not superseed existing values")

  (is (= [{:formula "zprint", :os :ubuntu, :package-manager :brew}]
         (sut/set-os [{:formula "zprint", :package-manager :brew}] :ubuntu)
         (sut/set-os [{:formula "zprint", :os :macos, :package-manager :brew}]
                     :ubuntu)
         (sut/set-os [{:formula "zprint", :os :all, :package-manager :brew}]
                     :ubuntu))
      "An os set replace existing values"))

(deftest extract-per-reqs-test
  (is (= {:developped {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]]}}
         (sut/extract-per-reqs {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]]}))
      "The simple cfg-item are considered developped already.")
  (is (= {:developped {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "cd"]],
                       :os 1,
                       :deps #{:test2 :test3 :d}},
          :to-develop {:test2 [{:clean-cmds ["rm" "-fr" "tmp"], :os 1}],
                       :test3 [{:clean-cmds ["rm" "-fr" "cache"], :os 1}]}}
         (sut/extract-per-reqs {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "cd"]],
                                :deps [:d],
                                :pre-reqs
                                {:test2 {:clean-cmds ["rm" "-fr" "tmp"]},
                                 :test3 {:clean-cmds ["rm" "-fr" "cache"]}},
                                :os 1}))
      "The `pre-reqs` in cfg-item is moved to `to-develop`"))

(deftest normalize-test
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                              ["rm" "-f" "cd"]]}}
         (sut/normalize {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                                             ["rm" "-f" "cd"]]}}
                        :macos
                        []))
      "Simple case for all os and not nested is normalized already.")
  (is
   (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
              :os :macos}}
      (sut/normalize
       {:test [{:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                :os :macos}
               {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                :os :linux}]}
       :macos
       []))
   "When multiple os are declared, only the one matching the current os are returned.")
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                 :os :macos,
                 :deps #{:test2 :test3 :test5}},
          :test2 {:clean-cmds ["rm" "-fr" "tmp"], :os :macos},
          :test3 {:clean-cmds ["rm" "-fr" "cache"], :os :macos}}
         (sut/normalize {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                                             ["rm" "-f" "cd"]],
                                :deps [:test5],
                                :pre-reqs
                                {:test2 {:clean-cmds ["rm" "-fr" "tmp"]},
                                 :test3 {:clean-cmds ["rm" "-fr" "cache"]
                                         :os :ubuntu}},
                                :os :macos}}
                        :macos
                        []))

      "Nested cfg-item in `pre-deps` ")
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                 :deps #{:test5 :test6 :test7},
                 :os :linux},
          :test6 {:clean-cmds ["rm" "-fr" "tmp"], :os :linux},
          :test7 {:clean-cmds ["rm" "-fr" "cache"], :os :linux}}
         (sut/normalize
          {:test
           [{:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
             :deps [:test4],
             :pre-reqs {:test2 {:clean-cmds ["rm" "-fr" "tmp"]},
                        :test3 {:clean-cmds ["rm" "-fr" "cache"]}},
             :os :macos}
            {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
             :deps [:test5],
             :pre-reqs {:test6 {:clean-cmds ["rm" "-fr" "tmp"]},
                        :test7 {:clean-cmds ["rm" "-fr" "cache"]}},
             :os :linux}]}
          :linux
          []))
      "Per os case with pre-reqs copied with their os"))

(deftest prepare-test
  (is (< 15
         (-> (sut/build [:doom] :macos)
             count))
      "The configuration file works"))

(deftest ordered-cfg-items-test
  (is (= ((juxt identity keys)
          {:docker-3 {:id 3},
           :docker-2 {:cfg-item-deps [:docker-3], :id 2},
           :docker-1 {:cfg-item-deps [:docker-2], :id 1},
           :docker {:cfg-item-deps [:docker-1], :id :none}})
         (let [cfg-items {:docker-3 {:id 3},
                          :docker-2 {:cfg-item-deps [:docker-3], :id 2},
                          :docker {:cfg-item-deps [:docker-1], :id :none},
                          :docker-1 {:cfg-item-deps [:docker-2], :id 1}}]
           ((juxt identity keys)
            (->> (sut/cfg-items-by-layers cfg-items)
                 (sut/ordered-cfg-items cfg-items)))))))
