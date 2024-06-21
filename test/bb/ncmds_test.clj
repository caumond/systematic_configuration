(ns ncmds-test
  (:require [ncmds :as sut]
            [clojure.test :refer [deftest is]]))

(deftest validate-cmd-test
  (is (not (sut/validate-cmd nil)) "Empty command not valid.")
  (is (not (sut/validate-cmd [12]))
      "Integers are not seen as valid comand tokens.")
  (is (sut/validate-cmd ["ls" "-la"]) "Valid command accepted."))

(deftest process-as-string-test
  (is (= nil (sut/execute-as-string ["non-existing-command"]))
      "Succesful operation returns string.")
  (is (string? (sut/execute-as-string ["ls"]))
      "Succesful operation returns string."))

(def ^:private s (new java.io.StringWriter))

(binding [*out* s]
  (deftest execute-process-test
    (is (= nil (sut/execute-cmd ["cd" ".."])) "cd is succesful.")
    (is
     (= [["non-existing-command"] true]
        ((juxt :cmd (comp some? :exception))
         (sut/execute-cmd ["non-existing-command"])))
     "non existing command is returning a map with the command in `:cmd` and an exception.")))

(deftest merge-cmds-test
  (is (= (sut/merge-cmds [["brew" "list" "--cask" "a" "b" "--versions"]
                          ["brew" "not-list" "--cask" "a" "c" "--versions"]
                          ["brew" "list" "a" "d" "--versions"]
                          ["brew" "list" "--cask" "e" "--versions"]]
                         ["brew" "list" "--cask"]
                         ["--versions"]
                         (constantly true))
         [["brew" "list" "--cask" "a" "b" "e" "--versions"]
          ["brew" "not-list" "--cask" "a" "c" "--versions"]
          ["brew" "list" "a" "d" "--versions"]]))
  (is (= (sut/merge-cmds [["brew" "list" "a" "b" "--versions"]
                          ["brew" "not-list" "--cask" "a" "c" "--versions"]
                          ["brew" "list" "a" "d" "--versions"]
                          ["brew" "list" "--cask" "e" "--versions"]]
                         ["brew" "list"]
                         ["--versions"]
                         #(not (contains? (set %) "--cask")))
         [["brew" "list" "a" "b" "a" "d" "--versions"]
          ["brew" "not-list" "--cask" "a" "c" "--versions"]
          ["brew" "list" "--cask" "e" "--versions"]])))
