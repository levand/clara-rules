(ns clara.tools.test-tracing
  (:require [clara.rules :refer :all]
            [clara.tools.tracing :as t]
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

    (is ( = [{:type :add-facts, :facts [(->Temperature 10 "MCI")]}]
            (t/get-trace session)))))
