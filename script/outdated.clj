(ns outdated
  (:require [antq.core :as antq]
            [lread.status-line :as status]))

(defn task
  [_opts]
  (status/line :head "Checking Clojure deps & Github Actions")
  (antq/-main))
