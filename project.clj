(defproject ordered "1.1.0"
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[clojure "[1.2.1,1.3.0]"]]
  :dev-dependencies [[ordered-set "0.2.2"] ; Not required except for benchmarks.
                     ]
  :tasks [cake-marginalia.tasks])
