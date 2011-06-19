(ns ordered.map
  (:use deftype.delegate :only [delegating-deftype])
  (:import (clojure.lang IPersistentMap
                         IPersistentVector
                         IEditableCollection
                         ITransientMap
                         IObj
                         IFn
                         MapEquivalence
                         Counted
                         Associative
                         Reversible
                         ILookup
                         ;; Indexed, maybe add later?
                         ;; Sorted almost certainly not accurate
                         )
           (java.util Map)))

(defn- remove-once [pred coll]
  (let [[before [x & after]] (split-with (complement pred) coll)]
    (concat before after)))

(declare transient-ordered-map)

(delegating-deftype OrderedMap [^IPersistentMap backing-map
                                ^IPersistentVector key-order]
  {backing-map {Counted [(count [])]
                IPersistentMap [(equiv [other])]
                Associative [(entryAt [k])
                             (valAt [k])
                             (valAt [k not-found])]
                IFn [(invoke [k])
                     (invoke [k not-found])]
                IObj [(meta [])
                      (withMeta [m])]
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
                         (conj key-order k)))))
  (assoc [this k v]
    (conj this [k v]))
  (without [this k]
           (if (contains? backing-map k)
             (OrderedMap. (dissoc backing-map k)
                          (vec (remove-once #(= k %) key-order)))
             this))
  (seq [this]
       (seq (map #(find backing-map %) key-order)))
  (iterator [this]
            (clojure.lang.SeqIterator. (seq this)))

  IEditableCollection
  (asTransient [this]
               (transient-ordered-map this))

  Reversible
  (rseq [this]
        (seq (OrderedMap. backing-map (rseq key-order)))))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (if (map? coll)
       (OrderedMap. coll (vec (keys coll))) ;; should be faster
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
  {backing-map {Counted [(count [])]
                ILookup [(valAt [k])
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
                                              (remove-once #(= % dissoc) order))
                                            key-order, dissocs))))))
  (conj [this e]
        (let [[k v] e]
          (assoc! this k v))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (transient (.backing-map om))
                        (transient (.key-order om))
                        (transient [])))
