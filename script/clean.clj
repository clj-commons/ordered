(ns clean
  (:require [babashka.fs :as fs]
            [lread.status-line :as status]))

(defn task
  [_opts]
  (status/line :head "Deleting build work")
  (status/line :detail "(d=deleted -=did not exist)")
  (run! (fn [d]
          (status/line :detail "[%s] %s"
                       (if (fs/exists? d) "d" "-")
                       d)
          (fs/delete-tree d {:force true}))
        ["target"
         ".cpcache"
         ".clj-kondo/.cache"
         ".lsp/.cache"
         ".eastwood"]))
