(ns test-native
  (:require [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]
            [babashka.fs :as fs]
            [babashka.tasks :as t]
            [clojure.edn :as edn]
            [clojure.math :as math]
            [clojure.string :as str]))

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

(defn get-classpath []
  (status/line :head "Get classpath")
  (let [classpath (-> "target/native-classpath.edn"
                      slurp
                      edn/read-string)]

    (println "\nClasspath:")
    (println (str "- " (str/join "\n- " classpath)))
    (str/join fs/path-separator classpath)))

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
    (t/clojure "-T:build jar")
    (status/line :head "AOT Compiling test sources against clojure %s" (:mvn-version clojure-version))
    (t/clojure "-T:build compile-clj-for-native-test :clj-version-alias" (:alias clojure-version))
    (let [classpath (get-classpath)]
      (run-native-image {:graal-native-image graal-native-image
                         :target-path target-path
                         :target-exe target-exe
                         :classpath classpath
                         :native-image-xmx native-image-xmx
                         :entry-class "flatland.ordered.native_test_runner"}))
    (status/line :head "Native image built")
    (let [full-target-exe (fs/which (fs/file target-path target-exe))]
      (status/line :detail "built: %s, %s" full-target-exe (humanize-bytes (fs/size full-target-exe)))
      (status/line :head "Running tests natively")
      (t/shell full-target-exe))))
