(ns build
  (:require [babashka.fs :as fs]
            [build-shared]
            [clojure.edn :as edn]
            [clojure.tools.build.api :as b]))

(def version (build-shared/lib-version))
(def lib (build-shared/lib-artifact-name))

(def class-dir "target/classes")
(def native-test-class-dir "target/native-test-classes") ;; keep this separate
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))

(defn compile-clj-for-native-test
  "We compile our tests against our local jar."
  [{:keys [clj-version-alias]}]
  (println "compile-clj to:" native-test-class-dir)
  (let [jars (->> (fs/glob "target" "*.jar") (mapv str))]
    (when (not= (count jars) 1)
      (throw (ex-info (format "Expected 1 jar under ./target to compile against, but found: %s"
                              (if (seq jars) jars "none"))
                      {})))
    (let [jar (first jars)
          basis (b/create-basis {:aliases [:native-test :test-common :clj-test-runner clj-version-alias]
                                 :extra {:deps {'clj-commons/ordered {:local/root jar}}}})]
      (println "Using jar:" jar)
      ;; share the classpath for native-image to use in test-native bb task
      (spit "target/native-classpath.edn" (pr-str (:classpath-roots basis)))
      (b/compile-clj {:basis basis
                      :class-dir native-test-class-dir
                      :src-dirs ["test"]
                      :ns-compile ['flatland.ordered.native-test-runner]}))))

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

(defn download-deps
  "Download all deps for all aliases"
  [_]
  (let [aliases (->> "deps.edn"
                     slurp
                     edn/read-string
                     :aliases
                     keys)]
    ;; one at a time because aliases with :replace-deps will... well... you know.
    (println "Bring down default deps")
    (b/create-basis {})
    (doseq [a (sort aliases)]
      (println "Bring down deps for alias" a)
      (b/create-basis {:aliases [a]}))))
