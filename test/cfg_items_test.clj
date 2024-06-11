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
    (is (some?
         (:package-manager
          (humanize sut/brew-package-manager
                    {:formula "aspell", :package-manager :brow})))
        "Wrong package manager is rejected.")
    (is (= {:version-cmds [["brew" "list" "aspell" "--versions"]],
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :init-cmds [],
            :install-cmds [["brew" "install" "aspell" "-q"]],
            :update-cmds [["brew" "upgrade" "aspell"]],
            :graph-deps [:brew]}
           (sut/brew-update {:formula "aspell", :package-manager :brew}))))
  (comment
    ;; With command
    (-> (sut/brew-update {:formula "aspell", :package-manager :brew})
        :version-cmds
        [ncmds/execute-cmd])
    ;
    )
  (testing "Formula with a tap."
    (is (= nil
           (humanize sut/brew-package-manager
                     {:package-manager :brew,
                      :formula "aspell",
                      :tap "d12frosted/emacs-plus"}))
        "The schema of formula and tap is accepted.")
    (is (= {:version-cmds ["brew" "list" "aspell" "--versions"],
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :init-cmds [],
            :install-cmds [["brew" "tap" "d12frosted/emacs-plus"]
                           ["brew" "install" "aspell" "-q"]],
            :package-manager :brew,
            :update-cmds [["brew" "upgrade" "aspell"]],
            :graph-deps [:brew]}
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
    (is (some?
         (:package-manager
          (humanize sut/npm-package-manager
                    {:package-manager :npm-old, :npm-deps ["typewritten"]})))
        "Wrong package manager is rejected")
    (is (= {:version-cmds ["npm" "version" "-g"],
            :check-cmds [],
            :clean-cmds [],
            :init-cmds [],
            :install-cmds [["npm" "install" "-g" "typewritten"]],
            :package-manager :npm,
            :update-cmds [["npm" "update" "-g" "typewritten"]],
            :graph-deps [:npm]}
           (sut/npm-update {:npm-dep "typewritten", :package-manager :npm}))
        "Valid formula return expected commands."))
  (comment
    ;; with command
    (-> (sut/npm-update {:formula "typewritten", :package-manager :npm})
        :version
        ncmds/execute-cmd)
    ;; With commands
    (-> (sut/npm-update {:formula "typewritten", :package-manager :npm})
        :version
        ncmds/execute-cmds)
    ;
    ))

(deftest manual-update-test
  (is (some? (humanize sut/manual-package-manager {:package-manager :manual}))
      "Missing installer is an error.")
  (is (= nil
         (humanize sut/manual-package-manager
                   {:package-manager :manual, :install-cmds [["pwd"]]}))
      "Valid manual package.")
  (is (some?
       (:package-manager
        (humanize sut/manual-package-manager
                  {:package-manager :manuel, :install-cmds [["pwd"]]})))
      "Invalid package manager is rejected")
  (comment
    (-> (sut/manual-update {:package-manager :manual, :install-cmds [["pwd"]]})
        :install-cmds)))

(deftest common-update-test
  (is (= {} (sut/common-update {})) "No common parameter is possible.")
  (is (= {:clean-cmds []} (sut/common-update {:tmp-files [], :clean-cmds []}))
      "No files deletions is ok.")
  (is (= {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"]]}
         (sut/common-update {:tmp-files ["a" "b"], :clean-cmds []}))
      "File deletion commands are ok."))

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
