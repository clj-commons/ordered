(ns dev-repl
  (:require [babashka.process :as process]
            [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]))

(defn- launch-repl [flavor {:keys [host bind port]}]
  (let [aliases (case flavor
                  :cljs "nrepl/cljs:cljs"
                  :jvm  "clj-kondo:build:nrepl/jvm")]
    (status/line :head "Launching Clojure %s nREPL" (name flavor))
    (process/exec "clj" (str "-M:" (:alias (clojure-versions/current-prod)) ":test-common:nrepl:" aliases)
                  "-h" host
                  "-b" bind
                  "-p" port)))

(def cli-repl-opts {:spec {:host {:ref "<ADDR>"
                                  :alias :h
                                  :default "127.0.0.1"
                                  :desc "Host address"}
                           :bind {:ref "<ADDR>"
                                  :alias :b
                                  :default "127.0.0.1"
                                  :desc "Bind address"}
                           :port {:ref "<symbols>"
                                  :coerce :int
                                  :default 0
                                  :alias :p
                                  :desc "Port, 0 for auto-select"}}})

;; Entry points
(defn dev-jvm
  {:org.babashka/cli cli-repl-opts}
  [opts]
  (launch-repl :jvm opts))

(defn dev-cljs
  {:org.babashka/cli cli-repl-opts}
  [opts]
  (launch-repl :cljs opts))
