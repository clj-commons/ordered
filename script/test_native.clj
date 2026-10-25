(ns test-native
  (:require [jar :as jar]
            [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]
            [babashka.fs :as fs]
            [babashka.tasks :as t]
            [clojure.math :as math]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(defn- find-graal-prog [prog-name]
  (or (fs/which prog-name)
      (fs/which (str (fs/file (System/getenv "JAVA_HOME") "bin")) prog-name)))

(defn- find-graal-native-image
  "The Graal team now bundle native-image with Graal, there is no longer any need to install it."
  []
  (status/line :head "Locate GraalVM native-image")
  (let [native-image (or (find-graal-prog "native-image")
                         (status/die 1 "failed to to find GraalVM native-image, it should be bundle with your Graal installation"))]
    (status/line :detail (str "found: " native-image))
    native-image))

(defn run-native-image [{:keys [:graal-native-image
                                :target-path :target-exe :classpath :native-image-xmx
                                :entry-class]}]
  (status/line :head "Graal native-image compile AOT")
  (let [full-path-target-exe (str (fs/file target-path target-exe))]
    (when-let [exe (fs/which full-path-target-exe)]
      (status/line :detail "Deleting existing %s" exe)
      (fs/delete exe))
    (let [native-image-cmd [graal-native-image
                            "-o" full-path-target-exe
                            "--features=clj_easy.graal_build_time.InitClojureClasses"
                            "-O1" ;; basic optimization for faster build
                            "-H:+ReportExceptionStackTraces"
                            "--verbose"
                            "-cp" classpath
                            (str "-J-Xmx" native-image-xmx)
                            entry-class]]
      (t/shell native-image-cmd))))

(defn humanize-bytes [bytes]
  (let [units ["bytes" "KB" "MB" "GB"]
        max-exponent (dec (count units))
        base 1024
        exponent (if (zero? bytes)
                   0
                   (int (math/floor (/ (math/log bytes) (math/log base)))))
        exponent (if (> exponent max-exponent)
                   max-exponent
                   exponent)
        in-bytes (format "%,d bytes" bytes)]
    (if (zero? exponent)
      in-bytes
      (format "%.2f %s (%s)"
              (/ bytes (math/pow base exponent))
              (units exponent)
              in-bytes))))

(defn- compile-clj-for-native-test
  "We compile our tests against our local jar."
  [{:keys! [jar-file github-coords]} clj-version-alias]
  (let [native-test-class-dir "target/native-test-classes"]
    (status/line :detail "compile-clj to: %s" native-test-class-dir)
    (let [basis (b/create-basis {:aliases [:native-test :test-common :clj-test-runner clj-version-alias]
                                 :extra {:deps {github-coords {:local/root jar-file}}}})]
      (status/line :detail "Using jar: %s" jar-file)
      (b/compile-clj {:basis basis
                      :class-dir native-test-class-dir
                      :src-dirs ["test"]
                      :ns-compile ['flatland.ordered.native-test-runner]})
      {:classpath (:classpath-roots basis)})))

(def cli-clojure-versions (mapv :version (clojure-versions/for-native)))

(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt cli-clojure-versions)}}
  [{:keys [clojure-version]}]
  (let [clojure-version (clojure-versions/lookup clojure-version)
        native-image-xmx "6g"
        target-path "target"
        target-exe "flatland-ordered-test"
        graal-native-image (find-graal-native-image)]
    (status/line :head "Creating native image for test")
    (status/line :detail "java -version")
    (t/shell "java -version")
    (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
    (fs/create-dirs target-path)
    (status/line :head "Creating ordered jar to test against")
    (let [config (jar/task {})]
      (status/line :head "AOT Compiling test sources against clojure %s" (:mvn-version clojure-version))
      (let [{:keys [classpath]} (compile-clj-for-native-test config (:alias clojure-version))]
        (status/line :head "Using classpath")
        (status/line :detail (str "- " (str/join "\n- " classpath)))
        (run-native-image {:graal-native-image graal-native-image
                           :target-path target-path
                           :target-exe target-exe
                           :classpath (str/join fs/path-separator classpath)
                           :native-image-xmx native-image-xmx
                           :entry-class "flatland.ordered.native_test_runner"}))
      (status/line :head "Native image built")
      (let [full-target-exe (fs/which (fs/file target-path target-exe))]
        (status/line :detail "built: %s, %s" full-target-exe (humanize-bytes (fs/size full-target-exe)))
        (status/line :head "Running tests natively")
        (t/shell full-target-exe)))))
