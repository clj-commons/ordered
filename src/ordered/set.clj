(ns ordered.set
  (:use [deftype.delegate :only [delegating-deftype]]
        [ordered.common :only [change!]])
  (:import (clojure.lang IPersistentSet IObj IEditableCollection
                         SeqIterator Reversible ITransientSet IFn
                         IPersistentVector ITransientVector RT)
           (java.util Set Collection)))

(declare ^{:private true} transient-ordered-set)

(delegating-deftype OrderedSet [^IPersistentSet backing-set
                                ^IPersistentVector order]
  {backing-set {IPersistentSet [(equiv [other])
                                (get [k])
                                (count [])]
                Object [(hashCode [])
                        (equals [other])]
                Set [(contains [k])
                     (containsAll [s])
                     (size [])
                     (isEmpty [])]
                IFn [(invoke [x])]}
   order {IPersistentSet [(seq [])]
          Reversible [(rseq [])]}}
  
  IPersistentSet
  (disjoin [this x]
    (if (.contains this x)
      (let [[before-split more-items]
            (loop [new-order (transient []), remaining (.seq this)]
              (let [y (first remaining), more (rest remaining)]
                (if (= x y)
                  [new-order more]
                  (recur (conj! new-order y) more))))
            
            final-order
            (loop [new-order before-split, remaining more-items]
              (if-let [more (seq remaining)]
                (recur (conj! new-order (first more)) (rest more))
                (persistent! new-order)))]
        (OrderedSet. (disj backing-set x) final-order))
      this))
  (cons [this x]
    (if (.contains this x)
      this
      (OrderedSet. (conj backing-set x)
                   (conj order x))))
  (empty [_]
    (OrderedSet. #{} []))

  IObj
  (meta [this]
    (.meta ^IObj backing-set))
  (withMeta [this m]
    (OrderedSet. (.withMeta ^IObj backing-set m)
                 order))

  Set
  (iterator [this]
    (SeqIterator. (.seq this)))
  (toArray [this dest]
    (let [len (.count this)]
      (if (> len (alength dest))
        (.toArray this)
        (do
          (reduce (fn [idx item]
                    (aset dest idx item)
                    (inc idx))
                  0, (.seq this))
          (when (> len (alength dest))
            (aset dest len nil))
          dest))))
  (toArray [this]
    (RT/seqToArray (.seq this)))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-set this)))

(def ^{:private true,
       :tag OrderedSet} empty-ordered-set (empty (OrderedSet. nil nil)))

(defn ordered-set
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(delegating-deftype TransientOrderedSet [^{:unsynchronized-mutable true,
                                           :tag ITransientSet} set,
                                         ^{:unsynchronized-mutable true,
                                           :tag ITransientVector} order]
  {set {ITransientSet [(count [])
                       (contains [x])
                       (get [x])]}}
  ITransientSet
  (conj [this x]
    (when (or (zero? (.count set))
              (not (.contains set x)))
      (change! set conj! x)
      (change! order conj! x))
    this)
  (disjoin [this x]
    (let [new-set (disj! set x)]
      (if (identical? new-set set)
        this
        (let [max (count order)]
          (set! set new-set)
          (loop [new-order (transient []), i 0]
            (if (= i max)
              (set! order new-order)
              (let [item (.valAt order i)]
                (recur (if (= item x)
                         new-order
                         (conj! new-order item))
                       (inc i)))))
          this))))
  (persistent [this]
    (OrderedSet. (persistent! set)
                 (persistent! order))))

(defn ^{:private true} transient-ordered-set [^OrderedSet os]
  (TransientOrderedSet. (transient (.backing-set os))
                        (transient (.order os))))
