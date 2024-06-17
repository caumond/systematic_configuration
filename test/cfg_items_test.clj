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
  (is (-> (m/schema sut/cfg-items-schema {:registry sut/registry})
          (humanize {:foo {}}))))

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
            :cfg-item-deps [:brew]
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :init-cmds [],
            :install-cmds [["brew" "install" "aspell" "-q"]],
            :update-cmds [["brew" "upgrade" "aspell"]]}
           (sut/brew-update {:formula "aspell", :package-manager :brew}))))
  (testing "Formula with a tap."
    (is (= nil
           (humanize sut/brew-package-manager
                     {:package-manager :brew,
                      :formula "aspell",
                      :tap "d12frosted/emacs-plus"}))
        "The schema of formula and tap is accepted.")
    (is (= {:cfg-version-cmds [["brew" "list" "aspell" "--versions"]]
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :cfg-item-deps [:brew]
            :init-cmds [],
            :install-cmds [["brew" "tap" "d12frosted/emacs-plus"]
                           ["brew" "install" "aspell" "-q"]],
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
    (is (= {:cfg-version-cmds []
            :cfg-item-deps [:npm]
            :check-cmds [["npm" "doctor" "typewritten"]],
            :clean-cmds [],
            :init-cmds [],
            :install-cmds [["npm" "install" "-g" "typewritten"]],
            :update-cmds [["npm" "update" "-g" "typewritten"]]}
           (sut/npm-update {:npm-deps ["typewritten"] :package-manager :npm}))
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
  (is (= {:cfg-version-cmds ["ls" "-la"]}
         (sut/manual-update {:package-manager :manual
                             :cfg-version-cmds ["ls" "-la"]}))
      "A manual package is cleaned from post-package, deps, pre-reqs, cfg-files tmp-files tmp-dirs and package-manager"))

(deftest common-update-test
  (is (= {} (sut/common-update {})) "It is possible to have no common parameter.")
  (is (= {:clean-cmds []}
         (sut/common-update {:tmp-files [], :clean-cmds []}))
      "No files deletions is ok.")
  (is (= {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["clean" "clean"]]}
         (sut/common-update {:tmp-files ["a" "b"], :clean-cmds [["clean" "clean"]]}))
      "File deletion and commands deletion are ok.")
  (is (= {:cfg-files ["a" "b"]}
         (sut/common-update {:cfg-files ["a" "b"]}))
      "Configuration files are copied"))
;;TODO Add tests for pre-reqs deps and tmp-dirs

(deftest tmp-files-update-test
  (is (= {:tmp-files ["a" "b" "cd"],
          :clean-cmds [["rm" "-fr" "a"] ["rm" "-fr" "b"] ["rm" "-fr" "cd"]]}
         (sut/common-update {:tmp-files ["a" "b" "cd"]}))))

(deftest expand-test
  (is (= {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]
                              ["rm" "-f" "cd"]]}}
         (sut/expand {:test {:tmp-files ["a" "b" "cd"]}})))
  (is (=
       {:test {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]]},
        :test2 {:version-cmds ["brew" "list" "black" "--versions"],
                :check-cmds [],
                :clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]],
                :init-cmds [],
                :install-cmds [["brew" "install" "black" "-q"]],
                :package-manager :brew,
                :update-cmds [["brew" "upgrade" "black"]],
                :graph-deps [:brew]}}
       (sut/expand {:test {:tmp-files ["a" "b" "cd"]},
                    :test2 {:tmp-files ["a" "b" "cd"],
                            :package-manager :brew,
                            :formula "black"}}))))

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

(deftest cfg-items-sorted-test
  (is (= [:docker :docker-1 :docker-2]
         (sut/cfg-items-sorted {:docker-2 {:pre-reqs {:docker-3 true}},
                                :docker {:pre-reqs {:docker-1 true}},
                                :docker-1 {:pre-reqs {:docker-2 true}}}))
      "Sort cfg-item according to their `:pre-reqs` dependency graph."))

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
           sut/expand)))
