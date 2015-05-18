(ns objective8.unit.drafts-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.storage.domain.drafts :refer :all]))

(def some-hiccup [[:p "barry"] [:p "brenda"] [:p] [:p nil] [:p {:class "test"} [:ul [:li "first item"]]]])
(def OBJECTIVE_ID 1)
(def DRAFT_ID 2)
(def DRAFT_URI (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))

(def SECTION_LABEL_1 "1234abcd")
(def SECTION_LABEL_2 "1234ef90")

(def SECTION_URI_1 (str DRAFT_URI "/sections/" SECTION_LABEL_1))
(def SECTION_URI_2 (str DRAFT_URI "/sections/" SECTION_LABEL_2))

(def some-hiccup-with-section-labels [["p" {:data-section-label SECTION_LABEL_1} "barry"] 
                                      ["ul" {:data-section-label SECTION_LABEL_2 :class "class"} 
                                       [:li "first item"]]])

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
      (get-section-labels-for-draft-uri DRAFT_URI) => [SECTION_LABEL_1 SECTION_LABEL_2]
      (provided
        (retrieve-draft-by-uri DRAFT_URI) => {:content some-hiccup-with-section-labels}))

(facts "about getting section from hiccup"
       (fact "gets section from hiccup"
             (get-section-from-hiccup some-hiccup-with-section-labels SECTION_LABEL_1) => [(first some-hiccup-with-section-labels)])

       (fact "fails nicely for empty hiccup elements"
             (get-section-from-hiccup [[:p]] "8") => nil) 

       (fact "returns nil when passing nil hiccup"
             (get-section-from-hiccup nil "12345678") => nil)) 

(def some-unordered-sections [{:uri SECTION_URI_2} {:uri SECTION_URI_1}])
(def SECTIONS_URI (str DRAFT_URI "/sections"))

(facts "about getting annotated sections"
       (fact "gets sections that have annotations with content in the order that they appear in the draft"
             (get-annotated-sections-with-section-content DRAFT_URI) 
             => [{:section [(first some-hiccup-with-section-labels)] 
                  :uri SECTION_URI_1}
                 {:section [(second some-hiccup-with-section-labels)] 
                  :uri SECTION_URI_2}] 
             (provided 
               (retrieve-annotated-sections SECTIONS_URI) => some-unordered-sections
               (retrieve-draft-by-uri DRAFT_URI) => {:content some-hiccup-with-section-labels})))
