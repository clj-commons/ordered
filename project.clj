(defproject org.flatland/ordered "1.5.7-SNAPSHOT"
  :url "https://github.com/clj-commons/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.flatland/useful "0.9.0"]]
  :aliases {"testall" ["with-profile" "dev,default:dev,1.6,default:dev,1.7,default:dev,1.8,default:dev,1.10,default" "test"]}
  :profiles {:1.10 {:dependencies [[org.clojure/clojure "1.10.0-RC3"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             ;; Not required except for benchmarks.
             :dev {:dependencies [[ordered-collections "0.4.2"]]} })
