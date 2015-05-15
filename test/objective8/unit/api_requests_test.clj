(ns objective8.unit.api-requests-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.api-requests :as ar]))

(def OBJECTIVE_ID 1)
(def QUESTION_ID 2)

(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))

(facts "request->answers-query"
       (tabular
        (fact "returns a query map when the request is valid"
              (ar/request->answers-query {:params ?params
                                          :route-params {:id OBJECTIVE_ID :q-id QUESTION_ID}})
              => ?answers-query)
        ?params                          ?answers-query
        {:sorted-by "created-at"}        {:sorted-by :created-at :filter-type :none :question-uri QUESTION_URI}
        {:sorted-by "up-votes"}          {:sorted-by :up-votes   :filter-type :none :question-uri QUESTION_URI}
        {:sorted-by "down-votes"}        {:sorted-by :down-votes :filter-type :none :question-uri QUESTION_URI}
        {:filter-type "has-writer-note"} {:sorted-by :created-at :filter-type :has-writer-note :question-uri QUESTION_URI}
        {:filter-type "has-writer-note"
         :sorted-by "up-votes"}          {:sorted-by :up-votes :filter-type :has-writer-note :question-uri QUESTION_URI})

       (tabular
        (fact "returns nil when the request is invalid"
              (ar/request->answers-query {:params ?params
                                          :route-params {:id OBJECTIVE_ID :q-id QUESTION_ID}})
              => nil)
        ?params
        {:sorted-by "invalid"}
        {:filter-type "invalid"}
        {:sorted-by "invalid" :filter-type "invalid"}))
