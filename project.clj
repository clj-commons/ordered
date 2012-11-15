(defproject org.flatland/ordered "1.3.3-SNAPSHOT"
  :url "https://github.com/flatland/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.flatland/useful "0.8.9-SNAPSHOT"]]
  ;; Not required except for benchmarks.
  :profiles {:dev {:dependencies [[ordered-collections "0.4.2"]]}})
