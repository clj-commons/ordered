(ns flatland.ordered.test-report
  (:require [clojure.test :as test]))

(def platform
  #?(:cljs (str "cljs " *clojurescript-version*)
     :clj  (str "jdk " (System/getProperty "java.vendor.version") " clj " (clojure-version))))

(defmethod test/report
  #?@(:cljs [[:cljs.test/default :begin-test-var]]
      :default [:begin-test-var]) [m]
  (let [test-name (-> m :var meta :name)]
    (println (str "=== " test-name " [" platform "]"))))
