(ns cfg-items-schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [malli.error :as me]
            [cfg-items-schema :as sut]
            [ncmds]))

(defn humanize
  [schema value]
  (-> schema
      (m/explain value)
      me/humanize))

(deftest brew-package-manager-test
  (testing "Formula without tap."
    (is (some? (:package-manager (humanize sut/brew-package-manager
                                           {:formula "aspell",
                                            :package-manager :brow})))
        "Wrong package manager is rejected.")
    (is (= nil
           (humanize sut/brew-package-manager
                     {:formula "aspell", :package-manager :brew}))
        "The schema of only one formula is accepted."))
  (testing "Formula with a tap."
    (is (= nil
           (humanize sut/brew-package-manager
                     {:package-manager :brew,
                      :formula "aspell",
                      :tap "d12frosted/emacs-plus"}))
        "The schema of formula and tap is accepted.")))

(deftest npm-update-test
  (is (= nil
         (humanize sut/npm-package-manager
                   {:package-manager :npm, :npm-deps ["typewritten"]}))
      "the schema of formula and tap is accepted.")
  (is (some? (:package-manager (humanize sut/npm-package-manager
                                         {:package-manager :npm-old,
                                          :npm-deps ["typewritten"]})))
      "Wrong package manager is rejected"))

(deftest manual-package-manager-test
  (is (= nil
         (humanize sut/manual-package-manager
                   {:package-manager :manual, :install-cmds [["pwd"]]}))
      "Valid manual package.")
  (is (some? (:package-manager (humanize sut/manual-package-manager
                                         {:package-manager :manuel,
                                          :install-cmds [["pwd"]]})))
      "Invalid package manager is rejected"))

(deftest cfg-item-schema-test
  (is (m/schema sut/cfg-items-schema {:registry sut/registry})
      "Raise an exception if schema is invalid.")
  (is (= nil
         (-> (m/schema sut/cfg-items-schema {:registry sut/registry})
             (humanize {})))))
