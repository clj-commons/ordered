(ns ordered.core
  (:import (clojure.lang IPersistentMap
                         IPersistentCollection
                         IPersistentVector
                         IObj
                         IFn
                         MapEquivalence
                         Counted
                         Associative
                         Reversible
                         ;; Indexed, maybe add later?
                         ;; Sorted almost certainly not accurate
                         )
           (java.util Map)))

;; TODO implement transient/persistent! with :volatile-mutable

(defn delegating-method [method-name args delegate]
  `(~method-name [~'this ~@args]
     (. ~delegate (~method-name ~@args))))

(defmacro delegating-deftype [cname [& fields] delegate-map & deftype-args]
  `(deftype ~cname [~@fields]
     ~@(apply concat
         (for [[interface methods] delegate-map]
           (cons interface
                 (for [[send-to which] methods
                       :let [send-to (vary-meta send-to
                                                assoc :tag interface)]
                       [name args] which]
                   (delegating-method name args send-to)))))
     ~@deftype-args))

(delegating-deftype OrderedMap [^IPersistentMap backing-map
                                ^IPersistentVector key-order
                                ^IPersistentMap meta-map]
  {Counted {backing-map [(count [])]}
   IPersistentCollection {backing-map [(equiv [other])]}
   Associative {backing-map [(entryAt [k])
                             (valAt [k])
                             (valAt [k not-found])]}
   Map {backing-map [(size [])
                     (get [k])
                     (containsKey [k])
                     (containsValue [v])
                     (isEmpty [])
                     (keySet [])
                     (values [])]}
   Object {backing-map [(equals [other])
                        (hashCode [])]}}

  ;; tagging interfaces
  MapEquivalence
  
  IPersistentMap
  (empty [this]
         (OrderedMap. {} [] {}))
  (cons [this obj]
        (let [[k v] obj
              new-map (assoc backing-map k v)]
          (OrderedMap. new-map
                       (if (contains? backing-map k)
                         key-order
                         (conj key-order k))
                       meta-map)))
  (assoc [this k v]
    (conj this [k v]))
  (seq [this]
       (seq (map #(find backing-map %) key-order)))
  (iterator [this]
            (clojure.lang.SeqIterator. (seq this)))

  Reversible
  (rseq [this]
        (seq (OrderedMap. backing-map (rseq key-order) meta-map)))

  IObj
  (meta [this]
        meta-map)
  (withMeta [this m]
            (OrderedMap. backing-map key-order m)))

(def ^OrderedMap empty-ordered-map (empty (OrderedMap. nil nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (into empty-ordered-map coll)))
