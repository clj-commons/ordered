(ns ordered.set
  (:use [ordered.map :only [ordered-map]])
  (:import (clojure.lang IPersistentSet IObj IEditableCollection
                         SeqIterator Reversible ITransientSet IFn)
           (java.util Set Collection)
           (ordered.map OrderedMap TransientOrderedMap)))

(declare transient-ordered-set)

(deftype OrderedSet [^OrderedMap backing-map]
  IPersistentSet
  (disjoin [this k]
    (OrderedSet. (.without backing-map k)))
  (cons [this k]
    (OrderedSet. (.assoc backing-map k k)))
  (seq [this]
    (seq (keys backing-map)))
  (empty [this]
    (OrderedSet. (ordered-map)))
  (equiv [this other]
    (.equals this other))
  (get [this k]
    (.get backing-map k))
  (count [this]
    (.count backing-map))

  IObj
  (meta [this]
    (meta backing-map))
  (withMeta [this m]
    (OrderedSet. (.withMeta backing-map m)))

  Object
  (hashCode [this]
    (reduce + (map hash (.seq this))))
  (equals [this other]
    (or (identical? this other)
        (and (instance? Set other)
             (let [^Set s (cast Set other)]
               (and (= (.size this) (.size s))
                    (every? #(.contains s %) this))))))

  Set
  (iterator [this]
    (SeqIterator. (.seq this)))
  (contains [this k]
    (.containsKey backing-map k))
  (containsAll [this ks]
    (every? identity (map #(.contains this %) ks)))
  (size [this]
    (.count this))
  (isEmpty [this]
    (zero? (.count this)))
  (toArray [this dest]
    (reduce (fn [idx item]
              (aset dest idx item)
              (inc idx))
            0, (.seq this))
    dest)
  (toArray [this]
    (.toArray this (object-array (count this))))

  Reversible
  (rseq [this]
    (seq (map key (rseq backing-map))))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-set this))
  IFn
  (invoke [_ k] (.invoke backing-map k))
  (invoke [_ k not-found] (.invoke backing-map k not-found)))

(def ^{:private true,
       :tag OrderedSet} empty-ordered-set (empty (OrderedSet. nil)))

(defn ordered-set
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(deftype TransientOrderedSet [^{:unsynchronized-mutable true
                                :tag TransientOrderedMap} backing-map]
  ITransientSet
  (count [this]
    (.count backing-map))
  (get [this k]
    (.valAt backing-map k))
  (disjoin [this k]
    (set! backing-map (dissoc! backing-map k))
    this)
  (conj [this k]
    (set! backing-map (assoc! backing-map k k))
    this)
  (contains [this k]
    (not (identical? this (.valAt backing-map k this))))
  (persistent [this]
    (OrderedSet. (persistent! backing-map))))

(defn transient-ordered-set [^OrderedSet os]
  (TransientOrderedSet. (transient (.backing-map os))))
