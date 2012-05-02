(ns ordered.common)

(def ^:dynamic *print-ordered* nil)

(defmacro change! [field f & args]
  `(set! ~field (~f ~field ~@args)))

(defprotocol Compactable
  (compact [this]))
