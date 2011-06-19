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
        (is (= other-way unsorted))))))

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
  (let [m (ordered-map {:a 1 :b 2 :c 3})]
    (testing "Keyword lookup"
      (is (= 1 (:a m))))
    (testing "Sequence views"
      (is (= [:a :b :c] (keys m)))
      (is (= [1 2 3] (vals m))))
    (testing "IFn support"
      (is (= 2 (m :b)))
      (is (= 'not-here (m :nothing 'not-here))))
    (testing "Get out Map.Entry"
      (is (= [:a 1] (find m :a))))
    (testing "Ordered dissoc"
      (let [m (dissoc m :b)]
        (is (= [:a :c] (keys m)))
        (is (= [1 3] (vals m)))))))

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
    (testing "Transients can dissoc!"
      (let [k (key (first m))
            t (transient m)
            t (dissoc! t k)]
        (is (= (persistent! t)
               (dissoc m k)))))))
