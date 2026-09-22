(ns flatland.ordered.eval-test
  "These tests are in this namespace to make them easy to skip when running tests under GraalVM.
  The make use of `eval` which is not a thing we can to in a GraalVM native image.
  I assume they are important, they were created in response to https://github.com/clj-commons/ordered/issues/55   "
  (:require [clojure.test :refer [deftest is]]
            [flatland.ordered.test-report]))

(deftest map-print-read-eval-ordered
  (is (= (pr-str (eval (read-string "#ordered/map[[:a 1] [:b 2]]")))
         "#ordered/map ([:a 1] [:b 2])"))
  (is (= (pr-str (eval (read-string "#ordered/map[[1 2] [3 4] [5 6] [1 9] [7 8]]")))
         "#ordered/map ([1 9] [3 4] [5 6] [7 8])")))

(deftest set-print-read-eval-ordered
  (is (= (seq (eval (read-string "#ordered/set (1 2 9 8 7 5)")))
         '(1 2 9 8 7 5)))
  (is (= (seq (eval (read-string "#ordered/set ([1 2] [3 4] [5 6] [1 9] [7 8])")))
         '([1 2] [3 4] [5 6] [1 9] [7 8]))))
