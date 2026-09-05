(ns flatland.ordered.map-test
  (:require [clojure.test :refer [deftest testing is are]]
            [flatland.ordered.map :refer [#?(:cljs OrderedMap) ordered-map]]
            #?(:clj [flatland.ordered.common :refer [compact]]
               :cljs [cljs.reader :as reader]))
  #?(:clj (:import flatland.ordered.map.OrderedMap)))

#?(:cljs
   (defn read-string [s]
     (reader/read-string {:readers {'ordered/map ordered-map}} s)))

(deftest implementations
  (let [basic (ordered-map)]
    #?(:clj
       (testing "Interfaces marked as implemented"
         (are [class] (instance? class basic)
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentCollection
           clojure.lang.Counted
           clojure.lang.Associative
           java.util.Map)))
    (testing "Behavior smoke testing"
      (testing "Most operations don't change type"
        (are [object] (= (type object) (type basic))
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
        (is (= {:meta :here}
               (-> basic
                   (with-meta {:meta :here})
                   (assoc :a :b)
                   (empty)
                   (meta))))
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
      (is (= #{one-item} (into #{} [one-item {1 2}]))))
    (testing "nil values don't break .equiv"
      (is (not= (ordered-map :x nil) {:y 0})))))

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
            expected (into values kvs)
            ordered (into m kvs)]
        (is (= (seq expected) (seq ordered)))))))

(deftest reversing
  (let [source (vec (for [n (range 10)]
                      [n n]))
        m (into (ordered-map) source)]
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
        (into m ()))))
  (let [m (ordered-map :a '("spark") :b '("flare" "bolt"))]
    (testing "assoc replaces values if not identical?"
      (is (vector? (-> m
                       (update-in [:a] vec)
                       (get :a)))))))

#?(:clj
   (deftest object-features
     (let [m (ordered-map 'a 1 :b 2)]
       (is (= "{a 1, :b 2}" (str m))))))

#?(:clj
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
             t1 t2)))
       (testing "Transients support ITransientAssociative2 methods"
         (let [t (transient m)]
           (is (contains? t 1))
           (is (= [1 2] (find t 1))))))))

(deftest print-and-read-ordered
  (let [s (ordered-map 1 2, 3 4, 5 6, 1 9, 7 8)]
    (is (= "#ordered/map ([1 9] [3 4] [5 6] [7 8])"
           (pr-str s)))
    (let [o (read-string (pr-str s))]
      (is (= OrderedMap (type o)))
      (is (= '([1 9] [3 4] [5 6] [7 8])
             (seq o))))))

#?(:clj
   (deftest print-read-eval-ordered
     (is (= (pr-str (eval (read-string "#ordered/map[[:a 1] [:b 2]]")))
            "#ordered/map ([:a 1] [:b 2])"))
     (is (= (pr-str (eval (read-string "#ordered/map[[1 2] [3 4] [5 6] [1 9] [7 8]]")))
            "#ordered/map ([1 9] [3 4] [5 6] [7 8])"))))

#?(:clj
   (deftest compacting
     (let [m1 (ordered-map :a 1 :b 2 :c 3)
           m2 (dissoc m1 :b)
           m3 (compact m2)
           m4 (dissoc m3 :c)]
       (is (= m2 (ordered-map :a 1 :c 3)))
       (is (= m3 m2))
       (is (= m4 (ordered-map :a 1))))))

#?(:clj
   (deftest same-hash
     (let [m1 (ordered-map :a 1 :b 2 :c 3)
           m2 (hash-map :a 1 :b 2 :c 3)
           m3 (array-map :a 1 :b 2 :c 3)]
       (is (= (hash m1) (hash m2) (hash m3)))
       (is (= (.hashCode m1) (.hashCode m2) (.hashCode m3)))
       (is (= (hash (ordered-map)) (hash (hash-map)) (hash (array-map))))
       (is (= (.hashCode (ordered-map)) (.hashCode (hash-map)) (.hashCode (array-map)))))))

#?(:clj
   (deftest nil-hash-code-npe
     ;; No assertions here; just check that it doesn't NPE
     ;; See: https://github.com/amalloy/ordered/issues/27
     (are [contents] (.hashCode (ordered-map contents))
       [[nil :a]]
       [[:a nil]]
       [[nil nil]])))
