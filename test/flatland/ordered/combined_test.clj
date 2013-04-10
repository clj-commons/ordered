(ns flatland.ordered.combined-test
  (:use clojure.test
        [flatland.ordered.map :only [ordered-map]]
        [flatland.ordered.set :only [ordered-set]]))

(defn version->= [vers-a vers-b]
  (>= (compare ((juxt :major :minor :incremental) vers-a)
               ((juxt :major :minor :incremental) vers-b))
      0))

(deftest test-duplicates-with-metadata
  (let [equal-sets-incl-meta (fn [s1 s2]
                               (and (= s1 s2)
                                    (let [ss1 (sort s1)
                                          ss2 (sort s2)]
                                      (every? identity
                                              (map #(and (= %1 %2)
                                                         (= (meta %1) (meta %2)))
                                                   ss1 ss2)))))
        all-equal-sets-incl-meta (fn [& ss]
                                   (every? (fn [[s1 s2]]
                                             (equal-sets-incl-meta s1 s2))
                                           (partition 2 1 ss)))
        equal-maps-incl-meta (fn [m1 m2]
                               (and (= m1 m2)
                                    (equal-sets-incl-meta (set (keys m1))
                                                          (set (keys m2)))
                                    (every? #(= (meta (m1 %)) (meta (m2 %)))
                                            (keys m1))))
        all-equal-maps-incl-meta (fn [& ms]
                                   (every? (fn [[m1 m2]]
                                             (equal-maps-incl-meta m1 m2))
                                           (partition 2 1 ms)))
        cmp-first #(> (first %1) (first %2))
        x1 (with-meta [1] {:me "x"})
        y2 (with-meta [2] {:me "y"})
        z3a (with-meta [3] {:me "z3a"})
        z3b (with-meta [3] {:me "z3b"})
        v4a (with-meta [4] {:me "v4a"})
        v4b (with-meta [4] {:me "v4b"})
        v4c (with-meta [4] {:me "v4c"})
        w5a (with-meta [5] {:me "w5a"})
        w5b (with-meta [5] {:me "w5b"})
        w5c (with-meta [5] {:me "w5c"})]

    ;; Sets
    ;; If there are duplicate items when doing (conj #{} x1 x2 ...),
    ;; the behavior is that the metadata of the first item is kept.
    (are [s x] (apply all-equal-sets-incl-meta s
                      (concat (if (version->= *clojure-version*
                                              {:major 1 :minor 5})
                                [ (apply hash-set x) ]
                                [])
                              [ (apply conj #{} x)
                                (into #{} x)
                                (apply ordered-set x)
                                (apply conj (ordered-set) x)
                                (into (ordered-set) x) ]))
      #{x1 y2} [x1 y2]
      #{x1 z3a} [x1 z3a z3b]
      #{w5b}    [w5b w5a w5c]
      #{z3a x1} [z3a z3b x1])

    ;; Maps
    ;; If there are duplicate keys when doing (assoc {} k1 v1 k2 v2
    ;; ...), the behavior is that the metadata of the first duplicate
    ;; key is kept, but mapped to the last value with an equal key
    ;; (where metadata of keys are not compared).
    (are [h x] (apply all-equal-maps-incl-meta h
                      (concat (if (version->= *clojure-version*
                                              {:major 1 :minor 5})
                                [ (apply hash-map x) ]
                                [])
                              [ (apply assoc {} x)
                                (into {} (map vec (partition 2 x)))
                                (apply ordered-map x)
                                (apply assoc (ordered-map) x)
                                (ordered-map (partition 2 x)) ]))
      {x1 2, z3a 4} [x1 2, z3a 4]
      {x1 2, z3a 5} [x1 2, z3a 4, z3b 5]
      {z3a 5}       [z3a 2, z3a 4, z3b 5]
      {z3b 4, x1 5} [z3b 2, z3a 4, x1 5]
      {z3b v4b, x1 5} [z3b v4a, z3a v4b, x1 5]
      {x1 v4a, w5a v4c, v4a z3b, y2 2} [x1 v4a, w5a v4a, w5b v4b,
                                        v4a z3a, y2 2, v4b z3b, w5c v4c])))
