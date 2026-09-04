(ns flatland.ordered.common)

(set! *warn-on-reflection* true)

(defmacro change! [field f & args]
  `(set! ~field (~f ~field ~@args)))

(defprotocol Compactable
  (compact [this]))
