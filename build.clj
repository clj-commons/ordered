(ns build
  (:require [babashka.fs :as fs]
            [build-shared]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def version (build-shared/lib-version))
(def lib (build-shared/lib-artifact-name))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))

(defn jar
  "Build library jar file.
  Supports `:version-override` for local testing, otherwise official version is used.
  For example, when testing 3rd party libs against rewrite-clj HEAD we use the suffix: canary."
  [{:keys [version-override] :as opts}]
  (b/delete {:path class-dir})
  (b/delete {:path jar-file})
  (let [version (or version-override version)]
    (println "jarring version" version)
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis basis
                  :src-dirs ["src"]
                  :scm {:url "https://github.com/clj-commons/ordered"
                        :connection "scm:git:git@github.com:clj-commons/ordered.git"
                        :developerConnection "scm:git:git@github.com:clj-commons/ordered.git"
                        :tag (format "v%s" version)}
                  :pom-data [[:description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."]
                             [:url "https://github.com/clj-commons/ordered"]
                             [:licenses
                              [:license
                               [:name "Eclipse Public License - v 1.0"]
                               [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
                             [:properties
                              [:project.build.sourceEncoding "UTF-8"]]]})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    (assoc opts :built-version version)))

(defn install
  "Install built jar to local maven repo, optionally specify `:version-override` for local testing."
  [opts]
  (let [{:keys [built-version]} (jar opts)]
    (println "installing version" built-version)
    (b/install {:class-dir class-dir
                :lib lib
                :version built-version
                :basis basis
                :jar-file jar-file})))

(defn deploy [_]
  (jar {})
  (println "deploy")
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   {:installer :remote
    :artifact jar-file
    :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

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
