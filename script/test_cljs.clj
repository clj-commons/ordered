(ns test-cljs
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;; On rewrite-clj I also tested under chrome-headless and planck in addition to node
;; - but planck is less interesting these days (and not maintained)
;; - and chrome-headless testing relied on karma which is now fully deprecated
(def valid-envs ["node"])
(def valid-optimizations ["none" "advanced"])

(defn compile-opts [out-dir {:keys [:env :optimizations]}]
  {:warnings {:fn-deprecated false}
   :target (when (= "node" env) :nodejs)
   :optimizations (keyword optimizations)
   :pretty-print (= "none" optimizations)
   :output-dir (str out-dir "/out")
   :output-to (str out-dir "/compiled.js")
   :source-map (= "none" optimizations)})

(defn run-tests [{:keys [:env :optimizations] :as opts}]
  (status/line :head "testing ClojureScript source under %s, cljs optimizations: %s" env optimizations)
  (let [test-combo (str env "-" optimizations)
        out-dir (str "target/cljsbuild/test/" test-combo)
        compile-opts-fname (str out-dir "-cljs-opts.edn")
        dep-aliases ":test-common:cljs:cljs-test"
        cmd (concat ["clojure"
                     (str "-M:" dep-aliases)]
                    ["--out" out-dir
                     "--env" env
                     "--compile-opts" compile-opts-fname])]
    (fs/delete-tree out-dir)
    (.mkdirs (io/file out-dir))
    (spit compile-opts-fname (compile-opts out-dir opts))
    (apply shell/command cmd)))

(defn task
  {:org.babashka/cli
   {:spec {:env {:alias :e
                 :coerce :string
                 :desc "JavaScript Environment"
                 :enum valid-envs
                 :default (first valid-envs)}
           :optimizations {:alias :o
                           :coerce :string
                           :desc "ClojureScript Optimizations"
                           :enum valid-optimizations
                           :default (first valid-optimizations)}}}}
  [opts]
  (run-tests opts))
