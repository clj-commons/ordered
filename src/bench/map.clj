(ns bench.map
  (:use [ordered.map :only [ordered-map]]))

(def map-size 1e6)

(defn gen-unique-kvs []
  (for [x (range map-size)]
    [x (- map-size x)]))

(defn conj-stress-test [& {:keys [m] :or {m (ordered-map)}}]
  (println "Testing conj")
  (=
   (time (reduce conj m (gen-unique-kvs)))))

(defn into-stress-test [& {:keys [m] :or {m (ordered-map)}}]
  (println "Testing into")
  (=
   (time (into m (gen-unique-kvs)))))

(defn dissoc-stress-test [& {:keys [m] :or {m (ordered-map)}}]
  (let [m (into m (gen-unique-kvs))
        ks (doall (keys m))]
    (println "Testing dissoc")
    (=
     (time (conj (reduce dissoc m ks)
                 [:a :b])))))

(defn transient-dissoc-test [& {:keys [m] :or {m (ordered-map)}}]
  (let [m (into m (gen-unique-kvs))
        ks (doall (keys m))
        t (transient m)]
    (println "Testing transient dissoc")
    (let [empty-transient (time (reduce dissoc! t ks))]
      (println "Testing persistent!")
      (= (time (conj (persistent! empty-transient)
                     [:a :b]))))))