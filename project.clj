(defproject org.flatland/ordered "1.5.4-SNAPSHOT"
  :url "https://github.com/amalloy/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure Clojure/ClojureScript implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [org.flatland/useful "0.9.0"]]
  ;;:aliases {"testall" ["with-profile" "dev,default:dev,1.3,default:dev,1.4,default:dev,1.5,default:dev,1.7,default" "test"]}
  :aliases {"testall" ["with-profile" "dev,default:dev,1.4,default:dev,1.5,default:dev,1.7,default:dev,1.7,cljs,default" "test"]
            "cljsbuild" ["with-profile" "dev,1.7,cljs,default" "cljsbuild"]}
  :clean-targets ^{:protect false} [:target-path "resources/compiled.js"]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0-RC1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             ;;:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             ;; Not required except for benchmarks.
             :dev {:dependencies [[ordered-collections "0.4.2"]]}
             :cljs {:plugins [[lein-cljsbuild "1.1.2"]]
                    :cljsbuild {:builds [{:source-paths ["src" "test"]
                                          :compiler {:output-to "resources/compiled.js"
                                                     :optimizations :whitespace}}]
                                :test-commands {"test" ["phantomjs"
                                                        "resources/test.js"
                                                        "resources/test.html"]}}
                    :hooks [leiningen.cljsbuild]}})
