(ns clara.tools.test-tracing
  (:require [clara.rules :refer :all]
            [clara.tools.tracing :as t]
            [clara.rules.engine :as eng]
            [clara.rules.dsl :as dsl]
            [clara.rules.testfacts :refer :all]
            [clojure.test :refer :all])

  (import [clara.rules.testfacts Temperature WindSpeed Cold TemperatureHistory
           ColdAndWindy LousyWeather First Second Third Fourth]))

(deftest test-simple-trace
  (let [rule-output (atom nil)
        cold-rule (dsl/parse-rule [[Temperature (< temperature 20)]]
                                  (reset! rule-output ?__token__))

        session (-> (mk-session [cold-rule] :listeners [(t/tracing-listener)] :cache false )
                    (insert (->Temperature 10 "MCI"))
                    (fire-rules))]

    ;; Ensure expected events occur in order.
    (is (= [:add-facts :right-activate :left-activate :add-activations]
           (map :type (t/get-trace session))))))

(deftest test-accumulate-trace
  (let [lowest-temp (accumulate
                     :reduce-fn (fn [value item]
                                  (if (or (= value nil)
                                          (< (:temperature item) (:temperature value) ))
                                    item
                                    value)))
        coldest-query (dsl/parse-query [] [[?t <- lowest-temp from [Temperature]]])

        session (-> (mk-session [coldest-query]  :listeners [(t/tracing-listener)] :cache false)
                    (insert (->Temperature 15 "MCI"))
                    (insert (->Temperature 10 "MCI"))
                    (insert (->Temperature 80 "MCI")))]



    (is (= [:add-facts :accum-reduced :left-activate :add-facts
            :left-retract :accum-reduced :left-activate :add-facts
            :left-retract :accum-reduced :left-activate]

           (map :type (t/get-trace session))))))
