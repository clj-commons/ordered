(ns flatland.ordered.set
  (:require-macros [flatland.ordered.common :refer [change!]])
  (:require [clojure.string :as s]
            [flatland.ordered.common :refer [Compactable]]
            [cljs.reader :refer [register-tag-parser!]]))

(declare transient-ordered-set)

(deftype OrderedSet [^IMap k->i
                     ^IVector i->k]

  ISet
  (-disjoin [this k]
    (if-let [i (k->i k)]
      (OrderedSet. (dissoc k->i k)
                   (assoc i->k i ::empty))
      this))

  ICollection
  (-conj [this k]
    (if (contains? k->i k)
        this
        (OrderedSet. (assoc k->i k (-count i->k))
                     (conj i->k k))))

  ISeqable
  (-seq [this]
    (seq (remove #(= ::empty %) i->k)))

  IEmptyableCollection
  (-empty [this]
    (OrderedSet. (-> {} (with-meta (meta k->i)))
                 []))

  IEquiv
  (-equiv [this other]
    (or (identical? this other)
        (and (satisfies? ISet other)
             (= (count this) (count other))
             (every? #(contains? other %) (seq this)))))

  ILookup
  (-lookup [this k]
    (when (contains? k->i k) k))
  (-lookup [this k not-found]
    (if (contains? k->i k) k not-found))

  ICounted
  (-count [this]
    (-count k->i))

  IMeta
  (-meta [this] (meta k->i))

  IWithMeta
  (-with-meta [this m]
    (OrderedSet. (with-meta k->i m)
                 i->k))

  Compactable
  (compact [this]
    (into (empty this) this))

  Object
  (toString [this]
    (str "#{" (clojure.string/join " " (map str this)) "}"))

  (equiv [this other]
    (-equiv this other))

  IHash
  (-hash [this]
    (hash (set this)))

  IReversible
  (-rseq [this]
    (seq (remove #(identical? ::empty %) (rseq i->k))))

  IEditableCollection
  (-as-transient [this]
    (transient-ordered-set this))

  IFn
  (-invoke [this k] (when (contains? k->i k) k))
  (-invoke [this k not-found] (if (contains? k->i k) k not-found))

  IPrintWithWriter
  (-pr-writer [this writer _]
    (-write writer (str "#ordered/set " (seq this)))))

(def ^{:private true,
       :tag OrderedSet} empty-ordered-set (empty (OrderedSet. nil nil)))

(defn ordered-set
  "Return a set with the given items, whose items are sorted in the
order that they are added. conj'ing an item that was already in the
set leaves its order unchanged. disj'ing an item and then later
conj'ing it puts it at the end, as if it were being added for the
first time. Supports transient.

Note that clojure.set functions like union, intersection, and
difference can change the order of their input sets for efficiency
purposes, so may not return the order you expect given ordered sets
as input."
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(deftype TransientOrderedSet [^{:unsynchronized-mutable true
                                :tag ITransientMap} k->i,
                              ^{:unsynchronized-mutable true
                                :tag ITransientVector} i->k]

  ICounted
  (-count [this]
    (-count k->i))

  ILookup
  (-lookup [this k] (-lookup k->i nil))
  (-lookup [this k not-found] (-lookup k->i not-found))

  ITransientSet
  (-disjoin! [this k]
    (let [i (-lookup k->i k)]
      (when i
        (change! k->i -dissoc! k)
        (change! i->k -assoc-n! i ::empty)))
    this)

  ITransientCollection
  (-conj! [this k]
    (let [i (-lookup k->i k)]
      (when-not i
        (change! ^ITransientAssociative k->i assoc! k (-count i->k))
        (change! i->k conj! k)))
    this)
  (-persistent! [this]
    (OrderedSet. (-persistent! k->i)
                 (-persistent! i->k))))

(defn transient-ordered-set [^OrderedSet os]
  (TransientOrderedSet. (transient (.-k->i os))
                        (transient (.-i->k os))))

(defn into-ordered-set
  [items]
  (into empty-ordered-set items))

(register-tag-parser! 'ordered/set into-ordered-set)
