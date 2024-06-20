(ns current-os "Returns the current os")

(def os-name-to-kw {"Mac OS X" :macos, "Linux" :ubuntu})

(defn current-os
  []
  (-> (System/getProperty "os.name")
      os-name-to-kw))

(comment
  (current-os)
  ;
)
