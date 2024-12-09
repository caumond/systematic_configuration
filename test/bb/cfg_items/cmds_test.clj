(ns cfg-items.cmds-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [malli.error :as me]
            [cfg-items.cmds :as sut]
            [ncmds]))

(defn humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(deftest brew-test
  (testing (is (= {:cfg-version-cmds [["brew" "list" "aspell" "--versions"]],
                   :check-cmds [],
                   :clean-cmds [["brew" "cleanup" "aspell"]],
                   :cfg-item-deps [:brew],
                   :init-cmds [],
                   :install-cmds [["brew" "reinstall" "aspell" "-q"]],
                   :update-cmds [["brew" "upgrade" "aspell"]]}
                  (sut/brew {:formula "aspell", :package-manager :brew}))
               "Normal use case"))
  (testing "Formula with a tap."
    (is (= {:cfg-version-cmds [["brew" "list" "aspell" "--versions"]],
            :check-cmds [],
            :clean-cmds [["brew" "cleanup" "aspell"]],
            :cfg-item-deps [:brew],
            :init-cmds [],
            :install-cmds [["brew" "tap" "d12frosted/emacs-plus"]
                           ["brew" "reinstall" "aspell" "-q"]],
            :update-cmds [["brew" "upgrade" "aspell"]]}
           (sut/brew {:package-manager :brew,
                      :formula "aspell",
                      :tap "d12frosted/emacs-plus"}))
        "Valid formula return expected commands.")))

(deftest npm-test
  (is (= {:cfg-version-cmds [],
          :cfg-item-deps [:npm],
          :check-cmds [["npm" "doctor" "typewritten"]],
          :clean-cmds [],
          :init-cmds [],
          :install-cmds [["npm" "install" "-g" "typewritten"]],
          :update-cmds [["npm" "update" "-g" "typewritten"]]}
         (sut/npm {:npm-deps ["typewritten"], :package-manager :npm}))
      "Valid formula return expected commands."))

(deftest manual-test
  (is
   (= {:cfg-version-cmds ["ls" "-la"]}
      (sut/manual {:package-manager :manual, :cfg-version-cmds ["ls" "-la"]}))
   "A manual package is cleaned from post-package, deps, pre-reqs, cfg-files tmp-files tmp-dirs and package-manager"))

(deftest common-test
  (is (= {} (sut/common {})) "It is possible to have no common parameter.")
  (is (= {}
         (sut/common {:clean-cmds []})
         (sut/common {:tmp-files [], :clean-cmds []})
         (sut/common {:tmp-dirs [], :clean-cmds []}))
      "No files deletions is ok.")
  (is (= {:clean-cmds [["clean" "clean"]]}
         (sut/common {:clean-cmds [["clean" "clean"]]}))
      "Clean commands are copied.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-f" "a"] ["rm" "-f" "b"]]}
         (sut/common {:tmp-files ["a" "b"], :clean-cmds [["clean" "clean"]]}))
      "File deletion and clean commands deletion are merged.")
  (is (= {:clean-cmds [["clean" "clean"] ["rm" "-fr" "a"] ["rm" "-fr" "b"]]}
         (sut/common {:tmp-dirs ["a" "b"], :clean-cmds [["clean" "clean"]]}))
      "File deletion and clean commands deletion are merged.")
  (is (= {:cfg-files ["a" "b"]} (sut/common {:cfg-files ["a" "b"]}))
      "Configuration files are copied")
  (is (= {:post-package {:a 1, :b 1}} (sut/common {:post-package {:a 1, :b 1}}))
      "Post package are copied")
  (is (= {:cfg-item-deps [:a :b]} (sut/common {:pre-reqs {:a 1, :b 1}}))
      "Pre reqs creates dependencies")
  (is (= {:cfg-item-deps [:a :b]} (sut/common {:deps [:a :b]}))
      "Deps are copied in dependencies")
  (is (= {:cfg-item-deps [:a :b :c :d]}
         (sut/common {:deps [:a :b], :pre-reqs {:c 2, :d 2, :b 1}}))
      "Deps and pre reqs are concatened")
  (is (= {:clean-cmds [["rm" "-f" "a"] ["rm" "-f" "b"] ["rm" "-f" "cd"]]}
         (sut/common {:tmp-files ["a" "b" "cd"]})))
  (is (= {:clean-cmds [["rm" "-fr" "a"] ["rm" "-fr" "b"] ["rm" "-fr" "cd"]]}
         (sut/common {:tmp-dirs ["a" "b" "cd"]}))))

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
