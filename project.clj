(defproject org.flatland/ordered "1.5.10"
  :url "https://github.com/clj-commons/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :aliases {"testall" ["with-profile" "+1.8:+1.9:+1.10.0:+1.10.1" "test"]
            "depsall" ["with-profile" "+1.8:+1.9:+1.10.0:+1.10.1" "deps"]}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_ordered_password
                                    :sign-releases true}]]
  :profiles {:1.10.1 {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :1.10.0 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             ;; Not required except for benchmarks.
             :dev {:dependencies [[ordered-collections "0.4.2"]]} })
