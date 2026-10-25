(ns jar
  (:require [babashka.fs :as fs]
            [build-config :as build-config]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [helper.git :as git]
            [lread.status-line :as status]))

(defn task
  [opts]
  (status/line :head "Jar")
  (let [{:keys! [lib version basis src-dirs class-dir jar-file url
                 description version-tag
                 scm-connection-url scm-dev-connection-url
                 license-name license-url] :as opts}
        (-> (build-config/config opts)
            (assoc :basis (b/create-basis {})))]
    (status/line :detail "creating jar: %s" jar-file)
    (fs/delete-tree class-dir)
    (fs/delete-if-exists jar-file)
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis basis
                  :src-dirs src-dirs
                  :scm {:url url
                        :connection scm-connection-url
                        :developerConnection scm-dev-connection-url
                        :tag (if (str/ends-with? version-tag "-SNAPSHOT")
                               (git/head-sha)
                               version-tag)}
                  :pom-data [[:description description]
                             [:url url]
                             [:licenses
                              [:license
                               [:name license-name]
                               [:url license-url]]]
                             [:properties
                              [:project.build.sourceEncoding "UTF-8"]]]})
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    opts))
