(ns test-clj
  (:require [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn run-unit-tests [{:keys [mvn-version alias] :as _clojure-version}]
  (status/line :head (str "testing clojure source against clojure v" mvn-version))
  (shell/command "clojure"
                 (str "-M:test-common:clj-test-runner:" alias)))

(def cli-clojure-versions (conj (mapv :version (clojure-versions/all)) "all"))

(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt cli-clojure-versions)}}
  [{:keys [clojure-version]}]
  (let [env-jdk-version (jdk/version)
        clojure-versions (if (= "all" clojure-version)
                             (clojure-versions/all)
                             [(clojure-versions/lookup clojure-version)])]
    (doseq [v clojure-versions]
      (if (and (= "all" clojure-version)
               (< (:major env-jdk-version) (:min-jdk-major v)))
        (status/line :warn "Skipping testing clojure version %s\nIt requires min JDK %s, found JDK %s"
                     (:mvn-version v) (:min-jdk-major v) (:version env-jdk-version))
        (run-unit-tests v)))))
