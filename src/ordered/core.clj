(ns ordered.core
  (:import (clojure.lang IPersistentMap
                         IPersistentVector
                         IObj
                         IFn
                         MapEquivalence
                         Counted
                         Associative)
           (java.util Map)))

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
   IPersistentMap {backing-map [(equiv [other])]}
   Associative {backing-map [(entryAt [k])
                             (valAt [k])
                             (valAt [k not-found])]}
   Map {backing-map [(size [])
                     (get [k])
                     (equals [other])
                     (containsKey [k])
                     (containsValue [v])
                     (isEmpty [])
                     (keySet [])
                     (values [])]}}

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

  IObj
  (meta [this]
        meta-map)
  (withMeta [this m]
            (OrderedMap. backing-map key-order m)))

(def ^OrderedMap testm (empty (OrderedMap. nil nil nil)))
