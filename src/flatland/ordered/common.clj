(ns flatland.ordered.common)

(set! *warn-on-reflection* true)

(defmacro ^:no-doc change! [field f & args]
  `(set! ~field (~f ~field ~@args)))

(defprotocol Compactable
  (compact [coll] "Returns ordered collection equal to `coll` with any deleted entries purged. Useful after many deletions to reclaim memory."))
