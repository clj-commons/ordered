(defproject org.flatland/ordered "1.5.4-SNAPSHOT"
  :url "https://github.com/amalloy/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.flatland/useful "0.9.0"]]
  ;;:aliases {"testall" ["with-profile" "dev,default:dev,1.3,default:dev,1.4,default:dev,1.5,default:dev,1.7,default" "test"]}
  :aliases {"testall" ["with-profile" "dev,default:dev,1.4,default:dev,1.5,default:dev,1.7,default" "test"]}
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             ;;:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             ;; Not required except for benchmarks.
             :dev {:dependencies [[ordered-collections "0.4.2"]]} })
