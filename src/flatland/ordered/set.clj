(ns flatland.ordered.set
  (:require [clojure.string :as str]
            [flatland.ordered.common :refer [change! Compactable]])
  (:import (clojure.lang IPersistentSet ITransientSet IEditableCollection
                         ITransientMap ITransientAssociative
                         ITransientVector IHashEq
                         Associative SeqIterator Reversible IFn IObj)
           (java.io Writer)
           (java.util Set)))

(set! *warn-on-reflection* true)

(declare transient-ordered-set)

(deftype OrderedSet [^clojure.lang.IPersistentMap k->i
                     ^clojure.lang.IPersistentVector i->k]
  IPersistentSet
  (disjoin [this k]
    (if-let [i (.valAt k->i k)]
      (OrderedSet. (dissoc k->i k)
                   (assoc i->k i ::empty))
      this))
  (cons [this k]
    (if (.valAt k->i k)
      this
      (OrderedSet. (.assoc ^Associative k->i k (.count i->k))
                   (.cons i->k k))))
  (seq [_this]
    (seq (remove #(identical? ::empty %) i->k)))
  (empty [_this]
    (OrderedSet. (with-meta {} (meta k->i)) []))
  (equiv [this other]
    (.equals this other))
  (get [_this k]
    (when (.valAt k->i k) k))
  (count [_this]
    (.count k->i))

  IObj
  (meta [_this]
    (.meta ^IObj k->i))
  (withMeta [_this m]
    (OrderedSet. (.withMeta ^IObj k->i m)
                 i->k))

  Compactable
  (compact [this]
    (into (empty this) this))

  Object
  (toString [this]
    (str "#{" (str/join " " (map str this)) "}"))
  (hashCode [this]
    (reduce + (keep #(when (some? %) (.hashCode ^Object %)) (.seq this))))
  (equals [this other]
    (or (identical? this other)
        (and (instance? Set other)
             (let [^Set s other]
               (and (= (.size this) (.size s))
                    (every? #(.contains s %) (.seq this)))))))

  IHashEq
  (hasheq [this]
    (hash-unordered-coll this))

  Set
  (iterator [this]
    (SeqIterator. (.seq this)))
  (contains [_this k]
    (.containsKey k->i k))
  (containsAll [this ks]
    (every? #(.contains this %) ks))
  (size [this]
    (.count this))
  (isEmpty [this]
    (zero? (.count this)))
  (^objects toArray [this ^objects dest]
    (loop [idx 0
           s (.seq this)]
      (when s
        (aset dest idx (first s))
        (recur (inc idx) (next s))))
    dest)
  (toArray [this]
    (.toArray this (object-array (.count this))))

  Reversible
  (rseq [_this]
    (seq (remove #(identical? ::empty %) (rseq i->k))))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-set this))
  IFn
  (invoke [this k] (when (.contains this k) k)))

(def ^{:private true,
       :tag OrderedSet} empty-ordered-set (empty (OrderedSet. nil nil)))

(defn ordered-set
  ;; REMINDER: Keep docstring and arglists exactly in sync with its ClojureScript counterpart
  "Return a set with the given `xs`, whose elements are sorted in the order
   that they are added. conj'ing an item that was already in the set leaves
   its order unchanged. disj'ing an item and then later conj'ing it puts it
   at the end, as if it were being added for the first time.

   *NOTE*: The `clojure.set` functions like union, intersection, and difference
   can change the order of their input sets for efficiency purposes, so may
   not return the order you expect given ordered sets as input.

   Clojure supports `transient` ordered sets, ClojureScript does not."
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(deftype TransientOrderedSet [^{:unsynchronized-mutable true
                                :tag ITransientMap} k->i,
                              ^{:unsynchronized-mutable true
                                :tag ITransientVector} i->k]
  ITransientSet
  (count [_this]
    (.count k->i))
  (get [_this k]
    (when (.valAt k->i k) k))
  (disjoin [this k]
    (when-let [i (.valAt k->i k)]
      (change! k->i .without k)
      (change! i->k .assocN i ::empty))
    this)
  (conj [this k]
    (let [i (.valAt k->i k)]
      (when-not i
        (change! ^ITransientAssociative k->i .assoc k (.count i->k))
        (change! i->k conj! k)))
    this)
  (contains [_this k]
    (boolean (.valAt k->i k)))
  (persistent [_this]
    (OrderedSet. (.persistent k->i)
                 (.persistent i->k))))

(defn- transient-ordered-set [^OrderedSet os]
  (TransientOrderedSet. (transient (.k->i os))
                        (transient (.i->k os))))

(defn  into-ordered-set
  ;; REMINDER: Keep docstring and arglists exactly in sync with its ClojureScript counterpart
  ;; NOTE: This fn is less interesting for Clojure, but we keep it for historical reasons.
  "Create a new ordered set from `elements`.

  Used for registering runtime tag parsers for ClojureScript, see [docs](/doc/01-user-guide.adoc#cljs)."
  [elements]
  (into empty-ordered-set elements))

(defn ^:no-doc into-ordered-set-reader-cljs
  "Called by data_readers (at compile time)"
  [elements]
  `(into-ordered-set ~(vec elements)))

(defmethod print-method OrderedSet [o ^Writer w]
  (.write w "#ordered/set ")
  (print-method (seq o) w))

(defn ordered-set?
  "Returns `true` if `x` is a flatland ordered set. Does not include `transient` variant."
  [x]
  (instance? OrderedSet x))
