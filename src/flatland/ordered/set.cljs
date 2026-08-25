(ns flatland.ordered.set
  (:require [clojure.string :as string]))

(declare equiv-impl)

(defn print-ordered-set [writer order opts]
  (if-let [ks (seq order)]
    (pr-sequential-writer writer
                          (fn [k w opts] (pr-seq-writer (list k) w opts))
                          "(" " " ")"
                          opts
                          ks)
    ;; matches clj, where (print-method (seq o) w) writes "nil" when empty
    (-write writer "nil")))

(deftype OrderedSet [elements order]
  Object
  (toString [this]
    (str "#{" (string/join " " (map str this)) "}"))
  (equiv [this that] (equiv-impl elements that))

  ;; js Set interface
  (keys [this] (es6-iterator (seq order)))
  (entries [this] (es6-set-entries-iterator (seq order)))
  (values [this] (es6-iterator (seq order)))
  (has [this k] (contains? elements k))
  (forEach [this f]
    (doseq [k order]
      (f k k this)))
  (forEach [this f use-as-this]
    (doseq [k order]
      (.call f use-as-this k k this)))

  ICloneable
  (-clone [_] (OrderedSet. elements order))

  IWithMeta
  (-with-meta [this new-meta]
    (if (identical? new-meta (meta elements))
      this
      (OrderedSet. (with-meta elements new-meta) order)))

  IMeta
  (-meta [_] (meta elements))

  ICollection
  (-conj [this k]
    (if (contains? elements k)
      this
      (OrderedSet. (conj elements k) (conj order k))))

  ISet
  (-disjoin [this k]
    (if (contains? elements k)
      ;; note: not (remove #{k}), which would never drop nil or false
      (OrderedSet. (disj elements k) (into [] (remove #(= k %)) order))
      this))

  IEmptyableCollection
  (-empty [this]
    (if (seq order)
      (OrderedSet. (empty elements) [])
      this))

  IEquiv
  (-equiv [_ that] (equiv-impl elements that))

  IHash
  (-hash [_] (hash elements))

  ISeqable
  (-seq [_] (seq order))

  IReversible
  (-rseq [_] (rseq order))

  ICounted
  (-count [_] (-count elements))

  ILookup
  (-lookup [_ k] (-lookup elements k))
  (-lookup [_ k not-found] (-lookup elements k not-found))

  IFn
  (-invoke [this k] (-lookup this k))
  (-invoke [this k not-found] (-lookup this k not-found))

  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (-write writer "#ordered/set ")
    (print-ordered-set writer order opts)))

(defn equiv-impl [elements that]
  (= elements (if (instance? OrderedSet that)
                (.-elements that)
                that)))

(def ^:private empty-ordered-set (OrderedSet. #{} []))

(defn ordered-set
  "Return a set with the given items, whose items are sorted in the
order that they are added. conj'ing an item that was already in the
set leaves its order unchanged. disj'ing an item and then later
conj'ing it puts it at the end, as if it were being added for the
first time.

Note that clojure.set functions like union, intersection, and
difference can change the order of their input sets for efficiency
purposes, so may not return the order you expect given ordered sets
as input."
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(defn into-ordered-set
  [items]
  (into empty-ordered-set items))
