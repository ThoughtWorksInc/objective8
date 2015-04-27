(ns objective8.unit.drafts-test
  (:require [midje.sweet :refer :all]
            [objective8.drafts :refer :all]))

(def some-hiccup [[:p "barry"] [:p "brenda"] [:p] [:p nil] [:p {:class "test"} [:ul [:li "first item"]]]])
(def DRAFT_ID 2)

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
      (get-section-labels-for-draft DRAFT_ID) => ["1234abcd" "1234ef90"]
      (provided
        (retrieve-draft DRAFT_ID) => {:content [["p" {:data-section-label "1234abcd"} "barry"] ["ul" {:data-section-label "1234ef90" :class "class"} [:li "first item"]]]}))
