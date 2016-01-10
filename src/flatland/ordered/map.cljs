(ns flatland.ordered.map
  (:require-macros [flatland.ordered.common :refer [change!]])
  (:require [clojure.string :as s]
            [flatland.ordered.common :refer [Compactable]]
            [cljs.reader :refer [register-tag-parser!]]))

(defn ^IMapEntry entry [k v i]
  [k [i v]])

(defn map-entry? [x]
  (satisfies? IMapEntry x))

(declare transient-ordered-map)

(deftype OrderedMap [^IMap backing-map
                     ^IVector order]

  ICounted
  (-count [this]
    (-count backing-map))

  Object
  (toString [this]
    (str "{" (s/join ", " (for [[k v] this] (str k " " v))) "}"))

  (equiv [this other]
    (-equiv this other))

  (keys [this]
    (keys backing-map))

  (values [this]
    (map val backing-map))

  (entry-at [this k]
    (let [v (get this k ::not-found)]
      (when (not= v ::not-found)
        ^IMapEntry [k v])))

  IEquiv
  (-equiv [this other]
    (and (satisfies? IMap other)
         (= (-count this) (-count ^IMap other))
         (every? (fn [^IMapEntry e]
                   (= (-val e) (get ^IMap other (-key e))))
                 (seq this))))

  ILookup
  (-lookup [this k]
    (-lookup this k nil))
  (-lookup [this k not-found]
    (if-let [^IMapEntry e (get ^IMap backing-map k)]
      (-val e)
      not-found))

  IFn
  (-invoke [this k]
    (-lookup this k))
  (-invoke [this k not-found]
    (-lookup this k not-found))

  IHash
  (-hash [this]
    (hash (into {} this)))

  IEmptyableCollection
  (-empty [this]
    (OrderedMap. (-> {} (with-meta (meta backing-map))) []))

  ICollection
  (-conj [this entry]
    (if (map-entry? entry)
        (-assoc this (-key entry) (-val entry))
        (loop [acc this
               entries (seq entry)]
          (if (nil? entries)
              acc
              (let [entry (first entries)]
                (if (map-entry? entry)
                    (recur (-assoc acc (-key entry) (-val entry))
                           (next entries))
                    (throw (js/Error. "conj on a map takes map entries or seqables of map entries"))))))))

  IAssociative
  (-assoc [this k v]
    (if-let [^IMapEntry e (-lookup ^IMap backing-map k)]
      (let [old-v (-val e)]
        (if (= old-v v)
            this
            (let [i (-key e)]
              (OrderedMap. (-conj backing-map (entry k v i))
                           (-assoc order i ^IMapEntry [k v])))))
      (OrderedMap. (-conj backing-map (entry k v (-count order)))
                   (-conj order ^IMapEntry [k v]))))

  (-contains-key? [this k]
    (-contains-key? backing-map k))

  IMap
  (-dissoc [this k]
    (if-let [^IMapEntry e (-lookup ^IMap backing-map k)]
      (OrderedMap. (-dissoc backing-map k)
                   (-assoc order (-key e) nil))
      this))

  ISeqable
  (-seq [this]
    (seq (keep identity order)))

  IMeta
  (-meta [this]
    (-meta ^IMeta backing-map))

  IWithMeta
  (-with-meta [this m]
    (OrderedMap. (-with-meta ^IWithMeta backing-map m) order))

  IEditableCollection
  (-as-transient [this]
    (transient-ordered-map this))

  IReversible
  (-rseq [this]
    (seq (keep identity (rseq order))))

  Compactable
  (compact [this]
    (into (empty this) this))

  IPrintWithWriter
  (-pr-writer [this writer _]
    (-write writer (str "#ordered/map " (seq this)))))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil)))

(defn ordered-map
  "Return a map with the given keys and values, whose entries are
sorted in the order that keys are added. assoc'ing a key that is
already in an ordered map leaves its order unchanged. dissoc'ing a
key and then later assoc'ing it puts it at the end, as if it were
assoc'ed for the first time. Supports transient."
  ([] empty-ordered-map)
  ([coll]
     (into empty-ordered-map coll))
  ([k v & more]
     (apply assoc empty-ordered-map k v more)))

;; contains? is broken for transients. we could define a closure around a gensym
;; to use as the not-found argument to a get, but deftype can't be a closure.
;; instead, we pass `this` as the not-found argument and hope nobody makes a
;; transient contain itself.

(deftype TransientOrderedMap [^{:unsynchronized-mutable true, :tag ITransientMap} backing-map,
                              ^{:unsynchronized-mutable true, :tag ITransientVector} order]
  ICounted
  (-count [this]
    (-count backing-map))

  ILookup
  (-lookup [this k] (-lookup this k nil))
  (-lookup [this k not-found]
    (if-let [^IMapEntry e (-lookup backing-map k)]
      (-val e)
      not-found))

  ITransientAssociative
  (-assoc! [this k v]
    (let [^IMapEntry e (-lookup backing-map k this)
          vector-entry ^IMapEntry [k v]
          i (if (identical? e this)
                (do (change! order -conj! vector-entry)
                    (dec (-count order)))
                (let [idx (-key e)]
                  (change! order -assoc! idx vector-entry)
                  idx))]
      (change! backing-map -conj! (entry k v i))
      this))

  ITransientMap
  (-dissoc! [this k]
    (let [^IMapEntry e (-lookup backing-map k this)]
      (when-not (identical? e this)
        (let [i (-key e)]
          (change! backing-map -dissoc! k)
          (change! order -assoc! i nil)))
      this))

  ITransientCollection
  (-conj! [this e]
    (let [[k v] e]
      (-assoc! this k v)))

  (-persistent! [this]
    (OrderedMap. (-persistent! backing-map)
                 (-persistent! order))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (-as-transient ^IEditableCollection (.-backing-map om))
                        (-as-transient ^IEditableCollection (.-order om))))

(register-tag-parser! 'ordered/map ordered-map)
