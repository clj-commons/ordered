(ns flatland.ordered.test-cljs
  (:require-macros [cljs.test :refer [run-all-tests]])
  (:require [cljs.test]))

(enable-console-print!)

(defn ^:export run []
  (run-all-tests #"flatland.*-test"))
