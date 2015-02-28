(ns clara.test-destructured-binding
  (:require [clojure.test :refer :all]
            [clara.rules :refer :all]))

(deftest test-destructured-binding
  (let [rule '{:name "clara.test-destructured-binding/test-destructured-binding"
               :lhs [{:args [[e a v]]
                      :type :foo
                      :constraints [(= e 1) (= v ?value)]}]
               :rhs (println "The value was" ?value)}]
    (-> (mk-session [rule] :fact-type-fn second)
        (insert [1 :foo 42])
        (fire-rules))))
