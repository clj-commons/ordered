(defproject ordered "0.3.0"
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[clojure "1.2.1"]
                 [clojure-contrib "1.2.0"]
                 [ordered-set "0.2.2"]]
  :dev-dependencies [[org.clojars.flatland/cake-marginalia "0.6.1"]]
  :tasks [cake-marginalia.tasks])
