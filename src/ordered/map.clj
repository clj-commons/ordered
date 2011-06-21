(ns ordered.map
  (:use [ordered.common :only [ensure-vector]]
        [deftype.delegate :only [delegating-deftype delegating-reify]])
  (:import (clojure.lang IPersistentMap
                         IPersistentCollection
                         IEditableCollection
                         ITransientMap
                         IObj
                         IFn
                         MapEquivalence
                         Reversible
                         ;; Indexed, maybe add later?
                         ;; Sorted almost certainly not accurate
                         )
           (java.util Map)))

(delegating-deftype OrderedMap [^IPersistentMap backing-map
                                ^IPersistentCollection key-order]
  {backing-map {IPersistentMap [(equiv [other])
                                (count [])
                                (entryAt [k])
                                (valAt [k])
                                (valAt [k not-found])]
                IFn [(invoke [k])
                     (invoke [k not-found])]
                Map [(size [])
                     (get [k])
                     (containsKey [k])
                     (containsValue [v])
                     (isEmpty [])
                     (keySet [])
                     (values [])]
                Object [(equals [other])
                        (hashCode [])]}}
  ;; tagging interfaces
  MapEquivalence
  
  IPersistentMap
  (empty [this]
    (OrderedMap. {} []))
  (cons [this obj]
    (let [[k v] obj
          new-map (assoc backing-map k v)]
      (OrderedMap. new-map
                   (if (contains? backing-map k)
                     key-order
                     (conj (ensure-vector key-order) k)))))
  (assoc [this k v]
    (conj this [k v]))
  (without [this k]
    (if (contains? backing-map k)
      (OrderedMap. (dissoc backing-map k)
                   (remove-once #(= k %) key-order))
      this))
  (seq [this]
    (seq (map #(find backing-map %) key-order)))
  (iterator [this]
    (clojure.lang.SeqIterator. (seq this)))

  IObj
  (meta [this]
    (meta backing-map))
  (withMeta [this m]
    (OrderedMap. (with-meta backing-map m)
                 key-order))

  IEditableCollection
  (asTransient [this]
    (let [[mutable-map mutable-order dissocs]
          (map (comp atom transient)
               [backing-map (ensure-vector key-order) []]),
          not-found (Object.)]
      (delegating-reify
          {@mutable-map {ITransientMap [(count [])
                                        (valAt [k])
                                        (valAt [k not-found])]
                         IFn [(invoke [k])
                              (invoke [k not-found])]}}
          ITransientMap
          (assoc [this k v]
            (when (identical? not-found (get @mutable-map k not-found))
              (swap! mutable-order conj! k))
            (swap! mutable-map assoc! k v)
            this)
  
          (without [this k]
            (when-not (identical? not-found
                                  (get @mutable-map k not-found))
              ;; not-found not returned, so it was already there
              (swap! mutable-map dissoc! k)
              ;; defer updating key-order until persistence, since it's expensive
              (swap! dissocs conj! k))
            this)
  
          (persistent [_]
            (OrderedMap. (persistent! @mutable-map)
                         (let [key-order (persistent! @mutable-order)
                               dissocs (seq (persistent! @dissocs))]
                           (if-not dissocs
                             key-order
                             (vec (reduce (fn [order dissoc]
                                            (remove-once #(= % dissoc)
                                                         order))
                                          key-order, dissocs))))))
          (conj [this e]
            (let [[k v] e]
              (assoc! this k v))))))

  Reversible
  (rseq [this]
    (seq (OrderedMap. backing-map
                      (rseq (ensure-vector key-order))))))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (if (map? coll)
       (OrderedMap. coll (keys coll)) ;; should be faster
       (into empty-ordered-map coll)))
  ([k v & more]
     (apply assoc empty-ordered-map k v more)))

