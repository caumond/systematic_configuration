(ns cfg-items-test
  (:require [clojure.test :refer [deftest is]]
            [cfg-items :as sut]
            [ncmds]))

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
         (sut/normalize
          {:test
           {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
            :deps [:test5],
            :pre-reqs {:test2 {:clean-cmds ["rm" "-fr" "tmp"]},
                       :test3 {:clean-cmds ["rm" "-fr" "cache"], :os :ubuntu}},
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
