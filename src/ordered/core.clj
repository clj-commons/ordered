(ns ordered.core
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

(letfn [;; given a mess of deftype specs, possibly with classes/interfaces
        ;; specified multiple times, collapse it into a map like
        ;; {interface => (method1 method2...)}.
        ;; needed because core.deftype only allows specifying a class ONCE,
        ;; so our delegating versions would clash with client's custom methods.
        (aggregate [decls]
          (loop [ret {}, curr-key nil, decls decls]
            (if-let [[x & xs] (seq decls)]
              (if (seq? x)
                (recur (update-in ret [curr-key] conj x),
                       curr-key, xs)
                (recur (update-in ret [x] #(or % ())),
                       x, xs))
              ret)))

        ;; Given a map returned by aggregate, spit out a flattened deftype body
        (explode [aggregated]
          (apply concat
                 (for [[k v] aggregated]
                   (cons k v))))

        ;; Output the method body for a delegating implementation
        (delegating-method [method-name args delegate]
          `(~method-name [~'this ~@args]
             (. ~delegate (~method-name ~@args))))]

  (defmacro delegating-deftype
    "Shorthand for defining a new type with deftype, which delegates the methods
you name to some other object or objects (usually a member field).

The delegate-map argument should be structured like:
{object-to-delegate-to {Interface1 [(method1 [])
                                    (method2 [foo bar baz])]
                        Interface2 [(otherMethod [other])]},
 another-object {Interface1 [(method3 [whatever])]}}.

This will cause your deftype to include an implementation of Interface1.method1
which does its work by forwarding to (.method1 object-to-delegate-to), and
likewise for the other methods. Arguments will be forwarded on untouched, and
you should not include a `this` parameter. Note especially that you can have
methods from Interface1 implemented by delegating to multiple objects if you
choose, and can also include custom implementations for the remaining methods of
Interface1 if you have no suitable delegate.

Arguments after `delegate-map` are as with deftype, although if deftype ever has
options defined for it, delegating-deftype may break with them."
    [cname [& fields] delegate-map & deftype-args]
    (let [our-stuff (for [[send-to interfaces] delegate-map
                          [interface which] interfaces
                          :let [send-to (vary-meta send-to
                                                   assoc :tag interface)]
                          [name args] which]
                      [interface (delegating-method name args send-to)])]
      `(deftype ~cname [~@fields]
         ~@(explode (aggregate (apply concat deftype-args our-stuff)))))))

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
                         (valAt [k not-found])]}}
  
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
