(ns flatland.ordered.native-test-runner
  "Test runner used for Graal native image tests.
  Namespace cannot be automatically discovered during a native image test,
  so we specify them explicitly so that they will be compiled in.
  Any new test namespaces will need to be manually added."
  (:gen-class)
  (:require
   [clojure.test :as t]
   [flatland.ordered.map-test]
   [flatland.ordered.set-test]))

(defn
  -main
  [& _args]
  (println "clojure version" (clojure-version))
  (println "java version" (System/getProperty "java.version"))
  (println
   "running native?"
   (= "executable" (System/getProperty "org.graalvm.nativeimage.kind")))
  (let
   [{:keys [fail error]}
    (apply
     t/run-tests
     ;; Notice we are deliberately skipping flatland.ordered.eval-test
     '(flatland.ordered.map-test
       flatland.ordered.set-test))]
    (System/exit (if (zero? (+ fail error)) 0 1))))
