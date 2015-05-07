(ns objective8.unit.drafts-test
  (:require [midje.sweet :refer :all]
            [objective8.drafts :refer :all]))

(def some-hiccup [[:p "barry"] [:p "brenda"] [:p] [:p nil] [:p {:class "test"} [:ul [:li "first item"]]]])
(def OBJECTIVE_ID 1)
(def DRAFT_ID 2)
(def DRAFT_URI (str "/objective/" OBJECTIVE_ID "/drafts/" DRAFT_ID))

(def some-hiccup-with-section-labels [["p" {:data-section-label "1234abcd"} "barry"] ["ul" {:data-section-label "1234ef90" :class "class"} [:li "first item"]]])

(fact "labels are added to non-empty hiccup sections"
      (let [result (add-section-labels some-hiccup)]
        (first result) => (just [:p (just {:data-section-label (just #"[0-9a-f]{8}")}) "barry"])  
        (second (second result)) => (just {:data-section-label (just #"[0-9a-f]{8}")}) 
        (nth result 2) => [:p] 
        (nth result 3) => [:p nil] 
        (second (nth result 4)) => (just {:class "test" :data-section-label (just #"[0-9a-f]{8}")})))

(fact "returns sequence of unique hex strings"
      (distinct (get-n-unique-section-labels 5)) => (five-of (just #"[0-9a-f]{8}")))

(fact "gets section labels for a draft"
      (get-section-labels-for-draft-uri DRAFT_URI) => ["1234abcd" "1234ef90"]
      (provided
        (retrieve-draft-by-uri DRAFT_URI) => {:content some-hiccup-with-section-labels}))

(facts "about getting section from hiccup"
       (fact "gets section from hiccup"
             (get-section-from-hiccup some-hiccup-with-section-labels "1234abcd") => [(first some-hiccup-with-section-labels)])

       (fact "fails nicely for empty hiccup elements"
             (get-section-from-hiccup [[:p]] "12345678") => nil) 

       (fact "returns nil when passing nil hiccup"
             (get-section-from-hiccup nil "12345678") => nil)) 
