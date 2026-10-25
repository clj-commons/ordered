(ns build
  "Anything that is only practical do in Clojure JVM goes here and is invoked by
  `clojure -T:build <fn-name> ...`"
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(defn lint-docstrings
  "Since we aren't using .cljc, our docstrings are copy-pasted between .clj and .cljs files.
  Verify docstrings are the the same and non-blank.
  Current checks satisfy current public API, adjust as necessary."
  ;; not currently checking clj only `compact` for blank, but no need, that's not something
  ;; we'll get tripped up on
  [_]
  (let [kondo-result ((requiring-resolve 'clj-kondo.core/run!)
                      {:lint ["src"]
                       :cache false
                       :skip-lint true
                       :config {:analysis {:arglists true
                                           :var-usages false
                                           :var-definitions {:meta true}}}})
        kondo-errors (->> kondo-result :findings (filter #(= :error (:level %))))]
    (if (seq kondo-errors)
      (throw (ex-info (format "lint-docstrings: clj-kondo errors: %s"
                              (into [] kondo-errors)) {}))
      (let [vars (->> kondo-result :analysis :var-definitions
                      (remove :private)
                      (remove #(-> % :meta :no-doc))
                      (filter #(#{"defn" "defmacro"} (-> % :defined-by name))))
            docstrings (reduce (fn [acc {:keys [filename ns name doc]}]
                                 (let [ext (fs/extension filename)
                                       lang (case ext
                                              "clj" :clj
                                              "cljs" :cljs)]
                                   (assoc-in acc [(symbol (str ns) (str name)) lang] doc)))
                               {}
                               vars)
            docstrings (update-vals docstrings
                                    (fn [{:keys [clj cljs] :as m}]
                                      (assoc m :result
                                             (cond (or (str/blank? clj) (str/blank? cljs)) :blank
                                                   (not= clj cljs) :mismatch
                                                   :else :pass))))
            docstrings (into (sorted-map) docstrings)
            pass? (every? #(= (:result %) :pass) (vals docstrings))]
        (println "Public API docstrings" (if pass? "all good" "have issue(s)"))
        (doseq [[v {:keys [clj cljs result]}] docstrings]
          (if (= :pass result)
            (println "✔️" v)
            (println "❌" v "PROBLEM:" result "\nclj: " clj "\n\ncljs:" cljs)))
        (when-not pass?
          (System/exit 1))))))
