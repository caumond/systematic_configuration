(ns ncmds-test
  (:require [ncmds :as sut]
            [clojure.test :refer [deftest is]]))

(deftest cmd-to-str-test (is (= "ls -la" (sut/cmd-to-str ["ls" "-la"]))))

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
    (is (= "Execute `ls`\n" (with-out-str (sut/execute-cmd ["ls"])))
        "A process prints what it's doing.")
    (is (= nil (sut/execute-cmd ["cd" ".."])) "cd is succesful.")
    (is
     (= [["non-existing-command"] true]
        ((juxt :cmd (comp some? :exception))
         (sut/execute-cmd ["non-existing-command"])))
     "non existing command is returning a map with the command in `:cmd` and an exception."))
  (deftest execute-processes-test
    (is (nil? (sut/execute-cmds [["ls"] ["ls"]]))
        "Valid command are executed one after the other.")
    (is (some? (sut/execute-cmds [["non-existing-command"] ["ls"]]))
        "Valid command are executed one after the other.")))
