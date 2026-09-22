(ns lint-docstrings
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn task
  [_opts]
  (status/line :head "docstrings: linting")
  (let [{:keys [exit]} (shell/clojure {:continue true} "-T:clj-kondo:build lint-docstrings")]
    (System/exit exit)))
