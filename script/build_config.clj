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
         default-branch "master"
         changelog-file "CHANGELOG.adoc"]
     {;; project specific
      :lib lib
      :default-branch default-branch
      :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order."
      :license-name "Eclipse Public License - v 1.0"
      :license-url "http://www.eclipse.org/legal/epl-v10.html"
      :github-coords github-coords
      :version version
      ;; this project started with no prefix, moved to "Release-", but we prefer "v" as a version tag prefix
      :legacy-version-tag-prefixes ["Release-" ""]
      :src-dirs ["src"]
      :changelog-file changelog-file
      :user-guide-file "doc/01-user-guide.adoc"

      ;; derived and/or less likely to change
      :url (str "https://github.com/" github-coords)
      :scm-connection (format "scm:git:https://github.com/%s.git" github-coords)
      :scm-dev-connection (format "scm:git:ssh://git@github.com/%s.git" github-coords)
      :version-tag-prefix version-tag-prefix
      :version-tag (str version-tag-prefix version)
      :class-dir "target/classes"
      :jar-file (format "target/%s-%s.jar" (name lib) version)
      :changelog-url (format "https://github.com/%s/blob/%s/%s" github-coords default-branch changelog-file)})))
