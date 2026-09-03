(ns outdated
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (shell/command {:continue true}
                 "clojure -M:outdated"))

(defn task
  [_opts]
  (check-clojure))
