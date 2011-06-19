(ns ordered.map
  (:use [ordered.common :only [ensure-vector]]
        [deftype.delegate :only [delegating-deftype]]
        [amalloy.utils.seq :only [remove-once]])
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

(declare transient-ordered-map)

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
               (transient-ordered-map this))

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

;; contains? is broken for transients. we could define a closure around a gensym
;; to use as the not-found argument to a get, but deftype can't be a closure.
;; instead, we pass `this` as the not-found argument and hope nobody makes a
;; transient contain itself.
(delegating-deftype TransientOrderedMap [^{:unsynchronized-mutable true} backing-map
                                         ^{:unsynchronized-mutable true} key-order
                                         ^{:unsynchronized-mutable true} dissocs]
  {backing-map {ITransientMap [(count [])
                               (valAt [k])
                               (valAt [k not-found])]
                IFn [(invoke [k])
                     (invoke [k not-found])]
                Object [(equals [other])
                        (hashCode [])]}}
  
  ITransientMap
  (assoc [this k v]
    (when (identical? this (get backing-map k this))
      ;; not-found returned, so it wasn't already there
      (set! key-order (conj! key-order k)))
    (set! backing-map (assoc! backing-map k v))
    this)
  
  (without [this k]
           (when-not (identical? this (get backing-map k this))
             ;; not-found not returned, so it was already there
             (set! backing-map (dissoc! backing-map k))
             ;; defer updating key-order until persistence, since it's expensive
             (set! dissocs (conj! dissocs k)))
           this)
  
  (persistent [this]
              (OrderedMap. (persistent! backing-map)
                           (let [key-order (persistent! key-order)
                                 dissocs (seq (persistent! dissocs))]
                             (if-not dissocs
                               key-order
                               (vec (reduce (fn [order dissoc]
                                              (remove-once #(= % dissoc)
                                                           order))
                                            key-order, dissocs))))))
  (conj [this e]
        (let [[k v] e]
          (assoc! this k v))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (transient (.backing-map om))
                        (transient (ensure-vector (.key-order om)))
                        (transient [])))
