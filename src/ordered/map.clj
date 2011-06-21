(ns ordered.map
  (:use [ordered.common :only [change!]]
        [deftype.delegate :only [delegating-deftype]])
  (:import (clojure.lang IPersistentMap
                         IPersistentCollection
                         IPersistentVector
                         IEditableCollection
                         ITransientMap
                         IObj
                         IFn
                         MapEquivalence
                         Reversible
                         MapEntry
                         ;; Indexed, maybe add later?
                         ;; Sorted almost certainly not accurate
                         )
           (java.util Map)))

(declare transient-ordered-map)

(delegating-deftype OrderedMap [^IPersistentMap k->v
                                ^IPersistentMap k->i
                                ^IPersistentVector i->kv
                                next-index]
  {k->v {IPersistentMap [(equiv [other])
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
    (OrderedMap. {} {} [] 0))
  (cons [this obj]
    (let [[k v] obj]
      (assoc this k v)))
  (assoc [this k v]
    (let [new-entry (MapEntry. k v)
          old-index (get k->i k)]
      (if old-index
        (OrderedMap. (assoc k->v k v)
                     k->i
                     (assoc i->kv old-index new-entry)
                     next-index)
        (OrderedMap. (assoc k->v k v)
                     (assoc k->i k next-index)
                     (assoc i->kv next-index new-entry)
                     (inc next-index)))))
  (without [this k]
    (if-let [i (get k->i k)]
      (OrderedMap. (dissoc k->v k)
                   (dissoc k->i k)
                   (assoc i->kv i nil)
                   next-index)
      this))
  (seq [this]
    (seq (keep identity i->kv)))
  (iterator [this]
    (clojure.lang.SeqIterator. (seq this)))

  IObj
  (meta [this]
    (meta k->v))
  (withMeta [this m]
    (OrderedMap. (with-meta k->v m) k->i i->kv next-index))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-map this))

  Reversible
  (rseq [this]
    (seq (keep identity (rseq i->kv)))))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (into empty-ordered-map coll))
  ([k v & more]
     (apply assoc empty-ordered-map k v more)))

;; contains? is broken for transients. we could define a closure around a gensym
;; to use as the not-found argument to a get, but deftype can't be a closure.
;; instead, we pass `this` as the not-found argument and hope nobody makes a
;; transient contain itself.
(delegating-deftype TransientOrderedMap [^{:unsynchronized-mutable true} k->v
                                         ^{:unsynchronized-mutable true} k->i
                                         ^{:unsynchronized-mutable true} i->kv
                                         ^{:unsynchronized-mutable true} next-index]
  {k->v {ITransientMap [(count [])
                          (valAt [k])
                          (valAt [k not-found])]
         IFn [(invoke [k])
              (invoke [k not-found])]}}

  ITransientMap
  (assoc [this k v]
    (let [old-index (get k->i k)
          new-entry (MapEntry. k v)]
      (change! k->v assoc! k v)
      (if old-index
        (change! i->kv assoc! old-index new-entry)
        (do (change! k->i assoc! k next-index)
            (change! i->kv assoc! next-index new-entry)
            (change! next-index inc)))
      this))

  (without [this k]
    (when-let [i (get k->i k)]
      (change! k->v dissoc! k)
      (change! k->i dissoc! k)
      (change! i->kv assoc! i nil))
    this)

  (persistent [this]
    (OrderedMap. (persistent! k->v)
                 (persistent! k->i)
                 (persistent! i->kv)
                 next-index))
  (conj [this e]
    (let [[k v] e]
      (assoc! this k v))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (transient (.k->v om))
                        (transient (.k->i om))
                        (transient (.i->kv om))
                        (.next-index om)))
