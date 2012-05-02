(defproject ordered "1.2.2"
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[clojure "1.4.0"]]
  ;; Not required except for benchmarks.
  :dev-dependencies [[ordered-collections "0.4.1"]])
