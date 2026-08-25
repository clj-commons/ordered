(ns flatland.ordered.set-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.set :as set]
            [flatland.ordered.set :refer [#?(:cljs OrderedSet) ordered-set into-ordered-set]]
            #?(:clj [flatland.ordered.common :refer [compact]]
               :cljs [cljs.reader :as reader]))
  #?(:clj (:import (flatland.ordered.set OrderedSet))))

#?(:cljs
   ;; into-ordered-set, not ordered-set: the latter is variadic,
   ;; so it would wrap the read collection in a one-element set.
   (defn read-string [s]
     (reader/read-string {:readers {'ordered/set into-ordered-set}} s)))

(deftest implementations
  (let [s (ordered-set)]
    #?(:clj
       (testing "Interfaces marked as implemented"
         (are [class] (instance? class s)
           clojure.lang.IPersistentSet
           clojure.lang.IPersistentCollection
           clojure.lang.Counted
           java.util.Set))
       :cljs
       (testing "Protocols marked as implemented"
         (is (set? s))
         (are [protocol] (satisfies? protocol s)
           ISet
           ICollection
           IEmptyableCollection
           ISeqable
           IReversible
           ICounted
           ILookup
           IEquiv
           IHash
           IMeta
           IWithMeta
           IFn)))
    (testing "Behavior smoke testing"
      (testing "Most operations don't change type"
        (are [object] (= (type object) (type s))
             (conj s 1 2)
             (disj s 1)
             (into s #{1 2})))
      (testing "Seq-oriented operations return nil when empty"
        (are [object] (nil? object)
             (seq s)
             (rseq s)))
      (testing "Metadata"
        (is (nil? (seq (meta s))))
        (is (= 10 (-> s
                      (with-meta {:size 10})
                      meta
                      :size)))
        (is (= {:succeeded true}
               (-> s
                   (vary-meta assoc :succeeded true)
                   meta)))
        (is (= {:meta :here}
               (-> s
                   (with-meta {:meta :here})
                   (conj :a)
                   (empty)
                   (meta))))
        (testing "Metadata doesn't affect other properties"
          (let [m (with-meta s {:a 1})]
            (is (instance? OrderedSet m))
            (is (= m s))))
        (testing "Metadata behaves like set's metadata"
          (let [meta-map {:meta 1}
                s1 (with-meta #{} meta-map)
                s2 (with-meta s meta-map)]
            (is (= (meta (conj s1 1 2))
                   (meta (conj s2 1 2))))))))))

(deftest equality
  (let [empty (ordered-set)
        one-item (conj empty 1)]
    (testing "Basic symmetric equality"
      (is (= #{} empty))
      (is (= empty #{}))
      (is (= #{1} one-item))
      (is (= one-item #{1})))
    (testing "Order-insensitive comparisons"
      (let [one-way (into empty [1 2 3 4])
            other-way (into empty [3 4 1 2])
            unsorted #{1 2 3 4}]
        (is (= one-way other-way))
        (is (= one-way unsorted))
        (is (= other-way unsorted))))))

(deftest ordering
  (let [values [[:first 10]
                [:second 20]
                [:third 30]]
        s (into (ordered-set) values)]
    (testing "Seq behaves like seq of a vector"
      (is (= (seq values) (seq s))))
    (testing "New values get added at the end"
      (let [entry [:fourth 40]]
        (is (= (seq (conj values entry))
               (seq (conj s entry))))))
    (testing "Re-adding keys leaves them in the same place"
      (is (= (seq s)
             (seq (conj s [:second 20])))))
    (testing "Large number of keys still sorted"
      (let [ints (range 5000)
            expected (into values ints)
            ordered (into s ints)]
        (is (= (seq expected) (seq ordered)))))))

(deftest reversing
  (let [source (vec (range 1000))
        s (into (ordered-set) source)]
    (is (= (rseq s) (rseq source)))))

(deftest set-features
  (let [s (ordered-set :a 1 :b 2 :c 3)]
    (testing "Keyword lookup"
      (is (= :a (:a s))))
    (testing "IFn support"
      (is (= :b (s :b))))
    (testing "Falsy lookup support"
      (is (= false (#{false 1} false))))
    (testing "Ordered disj"
      (is (= #{:a 1 2 3} (disj s :b :c))))))

(deftest falsy-elements
  (are [s x] (and (contains? s x)
                  (= x (get s x))
                  (= x (s x)))
    (ordered-set nil)   nil
    (ordered-set false) false)
  (testing "disj removes falsy elements"
    (is (= (ordered-set :a) (disj (ordered-set nil :a) nil)))
    (is (= (ordered-set :a) (disj (ordered-set false :a) false))))
  (testing "falsy elements keep their place"
    (is (= '(nil false 0 "") (seq (ordered-set nil false 0 ""))))
    (is (= 4 (count (ordered-set nil false 0 ""))))))

(deftest set-protocol-interop
  (is (set? (ordered-set 1 2)))
  (is (= #{1 3} (set/difference (ordered-set 1 2 3) (ordered-set 2))))
  (is (= #{2} (set/intersection (ordered-set 1 2) (ordered-set 2 3))))
  (is (= #{1 2 3} (set/union (ordered-set 1) (ordered-set 2 3)))))

(deftest object-features
  (let [s (ordered-set 'a 1 :b 2)]
    (is (= "#{a 1 :b 2}" (str s)))))

#?(:clj
   (deftest transient-support
     (let [s (ordered-set 1 2 7 8)]
       (testing "Basic transient conj!"
         (let [t (transient s)
               t (conj! t 4) ; add 4
               t (conj! t 4) ; do nothing, 4's already there
               t (conj! t 1) ; should do nothing
               p (persistent! t)]
           (is (= p (conj s 4)))))
       (testing "Transients still keep order"
         (let [t (transient s)
               t (conj! t 0)
               t (conj! t 1)
               p (persistent! t)]
           (is (= (concat (seq s) '(0)) ; adding 0 (at the end) but not 1
                  (seq p)))))
       (testing "Transients can disj!"
         (let [k (first s)
               t (transient s)
               t (disj! t k)]
           (is (= (persistent! t)
                  (disj s k)))))
       (testing "Can lookup in transients"
         (let [t (transient s)]
           (is (.contains t (first s))))))))

(deftest print-and-read-ordered
  (let [s (ordered-set 1 2 9 8 7 5)]
    (is (= "#ordered/set (1 2 9 8 7 5)"
           (pr-str s)))
    (let [o (read-string (pr-str s))]
      (is (= OrderedSet (type o)))
      (is (= '(1 2 9 8 7 5)
             (seq o)))))
  (testing "The empty set round-trips"
    (is (= "#ordered/set nil" (pr-str (ordered-set))))
    (is (= (ordered-set) (read-string "#ordered/set nil")))))

#?(:clj
   (deftest print-read-eval-ordered
     (is (= (seq (eval (read-string "#ordered/set (1 2 9 8 7 5)")))
            '(1 2 9 8 7 5)))
     (is (= (seq (eval (read-string "#ordered/set ([1 2] [3 4] [5 6] [1 9] [7 8])")))
            '([1 2] [3 4] [5 6] [1 9] [7 8])))))

#?(:clj
   (deftest compacting
     (let [s1 (ordered-set :a :b :c)
           s2 (disj s1 :b)
           s3 (compact s2)
           s4 (disj s3 :c)]
       (is (= s2 (ordered-set :a :c)))
       (is (= s3 s2))
       (is (= s4 (ordered-set :a))))))

(deftest same-hash
  (let [m1 (ordered-set :a :b :c)
        m2 (hash-set :a :b :c)]
    (is (= (hash m1) (hash m2)))
    (is (= (hash (ordered-set)) (hash (hash-set))))
    (is (= (hash (ordered-set nil)) (hash (hash-set nil))))
    (is (= (hash (ordered-set nil :a {:b nil})) (hash (hash-set nil :a {:b nil}))))
    (is (not= (hash (ordered-set nil)) (hash (hash-set false))))
    (is (not= (hash (ordered-set false)) (hash (hash-set nil))))
    (is (= (hash (ordered-set false nil)) (hash (hash-set nil false))))))

#?(:clj
   (deftest same-hash-clj
     (let [m1 (ordered-set :a :b :c)
           m2 (hash-set :a :b :c)]
       (is (= (.hashCode m1) (.hashCode m2)))
       (is (= (.hashCode (ordered-set)) (.hashCode (hash-set))))
       (is (= (.hashCode (ordered-set nil)) (.hashCode (hash-set nil))))
       (is (= (.hashCode (ordered-set nil :a {:b nil})) (.hashCode (hash-set nil :a {:b nil})))))))

#?(:clj
   (deftest nil-and-false-hashes
     (is (not= (.hashCode (ordered-set nil)) (.hashCode (hash-set false))))
     (is (not= (.hashCode (ordered-set false)) (.hashCode (hash-set nil))))
     (is (= (.hashCode (ordered-set false nil)) (.hashCode (hash-set nil false))))))

(deftest nil-hash-npe
  ;; No assertions here; just check that it doesn't NPE
  ;; See: https://github.com/amalloy/ordered/issues/27
  (are [contents] (integer? (hash (apply ordered-set contents)))
    [nil]
    [nil :a])
  #?(:clj
     (are [contents] (.hashCode (apply ordered-set contents))
       [nil]
       [nil :a])))
