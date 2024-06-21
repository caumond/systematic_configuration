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
           (sut/brew-update {:formula "aspell", :package-manager :brew} :macos))))
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
                             :tap "d12frosted/emacs-plus"}
                            :mac-os))
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
           (sut/npm-update {:npm-deps ["typewritten"], :package-manager :npm}
                           :mac-os))
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
                          :cfg-version-cmds ["ls" "-la"]}
                         :mac-os))
   "A manual package is cleaned from post-package, deps, pre-reqs, cfg-files tmp-files tmp-dirs and package-manager"))

(deftest common-update-test
  (is (= {} (sut/common-update {} :mac-os))
      "It is possible to have no common parameter.")
  (is (= {}
         (sut/common-update {:clean-cmds []} :mac-os)
         (sut/common-update {:tmp-files [], :clean-cmds []} :mac-os)
         (sut/common-update {:tmp-dirs [], :clean-cmds []} :mac-os))
      "No files deletions is ok.")
  (is (= {:clean-cmds [["clean" "clean"]]}
         (sut/common-update {:clean-cmds [["clean" "clean"]]} :mac-os))
      "Clean commands are copied.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-f" "a"] ["rm" "-f" "b"]]}
         (sut/common-update {:tmp-files ["a" "b"],
                             :clean-cmds [["clean" "clean"]]} :mac-os))
      "File deletion and clean commands deletion are merged.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-fr" "a"] ["rm" "-fr" "b"]]}
         (sut/common-update {:tmp-dirs ["a" "b"],
                             :clean-cmds [["clean" "clean"]]}
                            :mac-os))
      "File deletion and clean commands deletion are merged.")
  (is (= {:cfg-files ["a" "b"]} (sut/common-update {:cfg-files ["a" "b"]}
                                                   :mac-os))
      "Configuration files are copied")
  (is (= {:post-package {:a 1, :b 1}}
         (sut/common-update {:post-package {:a 1, :b 1}}
                            :mac-os))
      "Post package are copied")
  (is (= {:cfg-item-deps [:a :b]} (sut/common-update {:pre-reqs {:a 1, :b 1}}
                                                     :mac-os))
      "Pre reqs creates dependencies")
  (is (= {:cfg-item-deps [:a :b]} (sut/common-update {:deps [:a :b]}
                                                     :mac-os))
      "Deps are copied in dependencies")
  (is (= {:cfg-item-deps [:a :b :c :d]}
         (sut/common-update {:deps [:a :b], :pre-reqs {:c 2, :d 2, :b 1}}
                            :mac-os))
      "Deps and pre reqs are concatened"))

(deftest tmp-files-update-test
  (is (= {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]]}
         (sut/common-update {:tmp-files ["a" "b" "cd"]}
                            :mac-os)))
  (is (= {:clean-cmds [["rm" "-fr" "a"] ["rm" "-fr" "b"] ["rm" "-fr" "cd"]]}
         (sut/common-update {:tmp-dirs ["a" "b" "cd"]}
                            :mac-os))))

(deftest expand-test
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                              ["rm" "-f" "cd"]]}}
         (sut/expand-package-managers {:test {:tmp-files ["a" "b" "cd"]}}
                                      :mac-os)))
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
                                             :formula "black"}}
                                    :mac-os))))

(deftest read-configurations-test
  (is (< 20 (count (sut/read-configurations :linux)))
      "Linux configurations are found.")
  (is (< 20
         (-> (sut/read-configurations :macos)
             keys
             count))
      "Macos configurations are found."))

(deftest limit-configurations-test
  (is (= 1
         (count (-> {:docker {:pre-reqs {:dockutil {:formula "dockutil"}}}}
                    (sut/limit-configurations [:docker]))))
      "Limit configurations to existing one is ok.")
  (is (= {}
         (-> {:docker {:pre-reqs {:dockutil {:formula "dockutil"}}}}
             (sut/limit-configurations [:doom])))
      "If not found, it returns empty maps."))

(deftest cfg-test
  (is (< 15
         (-> (sut/read-configurations :macos)
             (sut/limit-configurations [:doom])
             sut/develop-pre-reqs
             keys
             count)))
  (is (< 15
         (-> (sut/read-configurations :macos)
             (sut/limit-configurations [:doom])
             sut/develop-pre-reqs
             (sut/expand-package-managers :mac-os)
             count))))

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
