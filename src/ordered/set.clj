(ns ordered.set
  (:use [deftype.delegate :only [delegating-deftype]]
        [ordered.map :only [ordered-map]])
  (:import (clojure.lang IPersistentSet IObj IEditableCollection
                         SeqIterator Reversible)
           (java.util Set Collection)
           ordered.map.OrderedMap))

(declare transient-ordered-set)

(delegating-deftype OrderedSet [^OrderedMap backing-map]
  {backing-map {IPersistentSet [(get [k])
                                (count [])]
                Collection [(size [])
                            (isEmpty [])]}}

  IPersistentSet
  (disjoin [this k]
           (OrderedSet. (.without backing-map k)))
  (cons [this k]
        (OrderedSet. (.assoc backing-map k k)))
  (seq [this]
       (seq (keys backing-map)))

  Collection
  (iterator [this]
            (SeqIterator. (.seq this)))
  (contains [this k]
            (.containsKey backing-map k))
  (containsAll [this ks]
               (every? identity (map #(.contains this %) ks)))
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
        (seq (map key (rseq backing-map)))))


