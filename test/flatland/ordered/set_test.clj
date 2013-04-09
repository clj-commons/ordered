(ns flatland.ordered.set-test
  (:use clojure.test
        [flatland.ordered.set :only [ordered-set]]
        [flatland.ordered.common :only [compact]])
  (:import (flatland.ordered.set OrderedSet)))

(deftest implementations
  (let [s (ordered-set)]
    (testing "Interfaces marked as implemented"
      (are [class] (instance? class s)
          clojure.lang.IPersistentSet
          clojure.lang.IPersistentCollection
          clojure.lang.Counted
          java.util.Set))
    (testing "Behavior smoke testing"
      (testing "Most operations don't change type"
        (are [object] (= (class object) (class s))
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
            ordered (into s ints)]
        (= (seq ints) (seq ordered))))))

(deftest reversing
  (let [source (vec (range 1000))
        s (into (sorted-set) source)]
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

(deftest object-features
  (let [s (ordered-set 'a 1 :b 2)]
    (is (= "#{a 1 :b 2}" (str s)))))

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
        (is (.contains t (first s)))))))

(deftest print-and-read-ordered
  (let [s (ordered-set 1 2 9 8 7 5)]
    (is (= "#ordered/set (1 2 9 8 7 5)"
           (pr-str s)))
    (let [o (read-string (pr-str s))]
      (is (= OrderedSet (type o)))
      (is (= '(1 2 9 8 7 5)
             (seq o))))))

(deftest compacting
  (let [s1 (ordered-set :a :b :c)
        s2 (disj s1 :b)
        s3 (compact s2)
        s4 (disj s3 :c)]
    (is (= s2 (ordered-set :a :c)))
    (is (= s3 s2))
    (is (= s4 (ordered-set :a)))))
