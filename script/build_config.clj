(ns build-config
  (:require [clojure.edn :as edn]))

(defn config
  ([] (config {}))
  ([{override-version :version}]
   (let [{lib :name :keys! [version github-coords & :name]}
         (-> (edn/read-string (slurp "deps.edn"))
             :aliases :neil :project)
         version (or override-version version)
         version-tag-prefix "v"
         default-branch "master"]
     {:lib lib
      :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
      :github-coords github-coords
      :url (str "https://github.com/" github-coords)
      :scm-connection-url (format "scm:git:https://github.com/%s.git" github-coords)
      :scm-dev-connection-url (format "scm:git:ssh://git@github.com/%s.git" github-coords)
      :license-name "Eclipse Public License - v 1.0"
      :license-url "http://www.eclipse.org/legal/epl-v10.html"
      :default-branch default-branch
      :version version
      :version-tag-prefix version-tag-prefix
      :version-tag (str version-tag-prefix version)
      ;; this project started with no prefix, moved to "Release-", but we prefer "v" as a version tag prefix
      :legacy-version-tag-prefixes ["Release-" ""]
      :src-dirs ["src"]
      :class-dir "target/classes"
      :jar-file (format "target/%s-%s.jar" (name lib) version)
      :changelog-url (format "https://github.com/%s/blob/%s/CHANGELOG.adoc" github-coords default-branch)
      :changelog-fname "CHANGELOG.adoc"
      :user-guide-fname "doc/01-user-guide.adoc"})))
