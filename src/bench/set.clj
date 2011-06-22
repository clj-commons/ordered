(ns bench.set
  (:use [ordered.common :only [Compactable compact]])
  (:require [ordered.set :as amalloy]
            [ordered-set :as ninjudd]))

(def size 1e6)

(defn gen-unique-kvs []
  (range size))

(extend-protocol Compactable
  clojure.lang.PersistentOrderedSet
  (compact [this] this))

(defn conj-stress-test [& {:keys [m] :or {m (amalloy/ordered-set)}}]
  (println "Testing conj")
  (=
   (time (reduce conj m (gen-unique-kvs)))))

(defn into-stress-test [& {:keys [m] :or {m (amalloy/ordered-set)}}]
  (println "Testing into")
  (=
   (time (into m (gen-unique-kvs)))))

(defn dissoc-stress-test [& {:keys [m] :or {m (amalloy/ordered-set)}}]
  (let [m (into m (gen-unique-kvs))
        ks (reverse (seq m))]
    (println "Testing disj")
    (=
     (time (compact (reduce disj m ks))))))

(defn transient-dissoc-test [& {:keys [m] :or {m (amalloy/ordered-set)}}]
  (let [m (into m (gen-unique-kvs))
        ks (reverse (seq m))
        t (transient m)]
    (println "Testing transient dissoc")
    (let [empty-transient (time (reduce disj! t ks))]
      (println "Testing persistent!")
      (= (time (compact (persistent! empty-transient)))))))
