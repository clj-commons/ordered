(ns ordered.map-test
  (:use clojure.test
        [ordered.map :only [ordered-map]])
  (:import ordered.map.OrderedMap))

(deftest implementations
  (let [basic (ordered-map)]
    (testing "Interfaces marked as implemented"
      (are [class] (instance? class basic)
          clojure.lang.IPersistentMap
          clojure.lang.IPersistentCollection
          clojure.lang.Counted
          clojure.lang.Associative
          java.util.Map))
    (testing "Behavior smoke testing"
      (testing "Most operations don't change type"
        (are [object] (= (class object) (class basic))
             (conj basic [1 2])
             (assoc basic 1 2)
             (into basic {1 2})))
      (testing "Seq-oriented operations return nil when empty"
        (are [object] (nil? object)
             (seq basic)
             (rseq basic)))
      (testing "Metadata"
        (is (nil? (seq (meta basic))))
        (is (= 10 (-> basic
                      (with-meta {:size 10})
                      meta
                      :size)))
        (is (= {:succeeded true}
               (-> basic
                   (vary-meta assoc :succeeded true)
                   meta)))
        (testing "Metadata doesn't affect other properties"
          (let [m (with-meta basic {:a 1})]
            (is (instance? OrderedMap m))
            (is (= m basic))))
        (testing "Metadata behaves like map's metadata"
          (let [meta-map {:meta 1}
                m1 (with-meta {} meta-map)
                m2 (with-meta basic meta-map)]
            (is (= (meta (assoc m1 1 2))
                   (meta (assoc m2 1 2))))))))))

(deftest equality
  (let [empty (ordered-map)
        one-item (assoc empty 1 2)]
    (testing "Basic symmetric equality"
      (is (= {} empty))
      (is (= empty {}))
      (is (= {1 2} one-item))
      (is (= one-item {1 2})))
    (testing "Order-insensitive comparisons"
      (let [one-way (into empty {1 2 3 4})
            other-way (into empty {3 4 1 2})
            unsorted {1 2 3 4}]
        (is (= one-way other-way))
        (is (= one-way unsorted))
        (is (= other-way unsorted))))
    (testing "Hash code sanity"
      (is (integer? (hash one-item)))
      (is (= #{one-item} (into #{} [one-item {1 2}]))))))

(deftest ordering
  (let [values [[:first 10]
                [:second 20]
                [:third 30]]
        m (ordered-map values)]
    (testing "Seq behaves like on a seq of vectors"
      (is (= (seq values) (seq m))))
    (testing "New values get added at the end"
      (let [entry [:fourth 40]]
        (is (= (seq (conj values entry))
               (seq (conj m entry))))))
    (testing "Changing old mappings leaves them at the same location"
      (let [vec-index [1]
            vec-key (conj vec-index 1)
            map-key (get-in values (conj vec-index 0))
            new-value 5]
        (is (= (seq (assoc-in values vec-key new-value))
               (seq (assoc m map-key new-value))))))
    (testing "Large number of keys still sorted"
      (let [kvs (for [n (range 5000)]
                  [(str n) n])
            ordered (into m kvs)]
        (= (seq kvs) (seq ordered))))))

(deftest reversing
  (let [source (vec (for [n (range 10)]
                      [n n]))
        m (into (sorted-map) source)]
    (is (= (rseq m) (rseq source)))))

(deftest map-features
  (let [m (ordered-map :a 1 :b 2 :c 3)]
    (testing "Keyword lookup"
      (is (= 1 (:a m))))
    (testing "Sequence views"
      (is (= [:a :b :c] (keys m)))
      (is (= [1 2 3] (vals m))))
    (testing "IFn support"
      (is (= 2 (m :b)))
      (is (= 'not-here (m :nothing 'not-here)))
      (is (= nil ((ordered-map {:x nil}) :x 'not-here))) )
    (testing "Get out Map.Entry"
      (is (= [:a 1] (find m :a))))
    (testing "Get out Map.Entry with falsy value"
      (is (= [:a nil] (find (ordered-map {:a nil}) :a))))
    (testing "Ordered dissoc"
      (let [m (dissoc m :b)]
        (is (= [:a :c] (keys m)))
        (is (= [1 3] (vals m)))))
    (testing "Can conj a map"
      (is (= {:a 1 :b 2 :c 3 :d 4} (conj m {:d 4}))))
    (testing "(conj m nil) returns m"
      (are [x] (= m x)
           (conj m nil)
           (merge m ())
           (into m ())))))

(deftest object-features
  (let [m (ordered-map 'a 1 :b 2)]
    (is (= "{a 1, :b 2}" (str m)))))

(deftest transient-support
  (let [m (ordered-map {1 2 7 8})]
    (testing "Basic transient conj!"
      (let [t (transient m)
            t (conj! t [3 4])
            t (conj! t [3 4])
            p (persistent! t)]
        (is (= p (conj m [3 4])))))
    (testing "Transients still keep order"
      (let [t (transient m)
            t (assoc! t 0 1)
            p (persistent! t)]
        (is (= (concat (seq m) '([0 1]))
               (seq p)))))
    (testing "Transients can overwrite existing entries"
      (let [t (transient m)
            t (assoc! t 1 5)
            p (persistent! t)]
        (is (= p (assoc m 1 5)))))
    (testing "Transients can dissoc!"
      (let [k (key (first m))
            t (transient m)
            t (dissoc! t k)]
        (is (= (persistent! t)
               (dissoc m k)))))
    (testing "Can't edit transient after calling persistent!"
      (let [more [[:a 1] [:b 2]]
            t (transient m)
            t (reduce conj! t more)
            p (persistent! t)]
        (is (thrown? Throwable (assoc! t :c 3)))
        (is (= (into m more) p))))
    (testing "Transients are never equal to other objects"
      (let [[t1 t2 :as ts] (repeatedly 2 #(transient m))
            holder (apply hash-set ts)]
        (is (not= t1 t2))
        (is (= (count ts) (count holder)))
        (are [t] (= t (holder t))
             t1 t2)))))

(deftest print-and-read-ordered
  (let [s (ordered-map 1 2, 3 4, 5 6, 1 9, 7 8)]
    (is (= "#ordered/map ([1 9] [3 4] [5 6] [7 8])"
           (pr-str s)))
    (let [o (read-string (pr-str s))]
      (is (= ordered.map.OrderedMap (type o)))
      (is (= '([1 9] [3 4] [5 6] [7 8])
             (seq o))))))
