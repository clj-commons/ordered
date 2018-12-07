(ns flatland.ordered.generative-test
  (:require [clojure.test :refer :all]
            [collection-check.core :as cc]
            [flatland.ordered.map :as map]
            [flatland.ordered.set :as set]
            [clojure.test.check.generators :as gen]))

(def gen-element
  (gen/tuple gen/int))

(deftest test-sets
  (cc/assert-set-like 100 #{} gen-element)
  (cc/assert-set-like 100 (set/ordered-set) gen-element)
  (cc/assert-set-like 10 (set/ordered-set) gen/any))

(deftest test-maps
  (cc/assert-map-like 100 (map/ordered-map) gen-element gen-element)
  (cc/assert-map-like 10 (map/ordered-map) gen/any gen/any))
