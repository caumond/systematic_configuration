(ns current-os
  "Returns the current os

  Use hostnamectl to have a clearer idea of the distribution's name")

(def os-name-to-kw {"Mac OS X" :macos
                    "Linux" :ubuntu})

(defn current-os
  []
  ()
  (-> (System/getProperty "os.name")
      os-name-to-kw))

(comment
  (current-os)
  ;
)
