(ns objective8.unit.draft-diffs-test
  (:require [midje.sweet :refer :all]
            [objective8.utils :as utils]  
            [objective8.draft-diffs :as diffs]))

(def HICCUP_1 [["p" nil ""] ["p" nil "First paragraph."] ["ul" ["li" "List item 1."] ["li" "List item 2."] ["li" "List item 3."]] ["p" {} "Last paragraph."]])
(def HICCUP_2 [["p" {} "First paragraph."] ["p" {} "Second paragraph."] ["p" {} "Third paragraph."]])
(def HICCUP_3 [["p" {} "First paragraph."] ["p" {} "New paragraph."] ["p" {} "Third paragraph."]])

(def HICCUP_WITH_ELEMENT_WITH_MULTIPLE_STRINGS [["p" "string one" " string two" " string three"] ["p" "another-string"]])
(def CONTENT_STRING_FOR_MULTIPLE_STRINGS "string one string two string threeanother-string")

(def HICCUP_WITH_EMPTY_ELEMENT [["p" {}] ["p" "barry"]])
(def CONTENT_STRING_FOR_EMPTY_ELEMENT "barry")

(def ELEMENT [:del {} "First sentence. Second sentence."]) 

(def DIFF_1 '([:span nil "Equal text. "] [:ins nil "New text."]))
(def DIFF_2 '([:span nil "First paragraph."] [:del nil "Second paragraph."] [:span nil "Third paragraph."]))

(def FORMATTED_DIFF_1 '([:p [:span nil "Equal text. "] [:ins nil "New text."]])) 
(def FORMATTED_DIFF_2 '(["p" [:span nil "First paragraph."]] ["p" [:del nil "Second paragraph."]] ["p" [:span nil "Third paragraph."]]))
(def FORMATTED_PREVIOUS_DIFF_2_vs_3 '(["p" {} [:span nil "First paragraph."]] ["p" {} [:del nil "Second"] [:span nil " paragraph."]] ["p" {} [:span nil "Third paragraph."]]))
(def FORMATTED_CURRENT_DIFF_2_vs_3 '(["p" {} [:span nil "First paragraph."]] ["p" {} [:ins nil "New"] [:span nil " paragraph."]] ["p" {} [:span nil "Third paragraph."]]))
(def FORMATTED_PREVIOUS_DIFF_1_vs_2 '(["p" nil ""] ["p" nil [:span nil "First paragraph."]] ["ul" ["li" [:del nil "List item 1."]] ["li" [:del nil "List item 2."]] ["li" [:del nil "List item 3."]]] ["p" {} [:del nil "Last"] [:span nil " paragraph."]]))
(def FORMATTED_CURRENT_DIFF_1_vs_2 '(["p" {} [:span nil "First paragraph."]]  ["p" {} [:ins nil "Second paragraph."]] ["p" {} [:ins nil "Third"] [:span nil " paragraph."]]))

(fact "Convert hiccup to content-string for simple hiccup"
      (diffs/hiccup->content-string HICCUP_2) => "First paragraph.Second paragraph.Third paragraph.")

(fact "Convert hiccup to content-string with nested tags"
      (diffs/hiccup->content-string HICCUP_1) => "First paragraph.List item 1.List item 2.List item 3.Last paragraph.")

(fact "Convert hiccup to content-string when an element has multiple strings" 
      (diffs/hiccup->content-string HICCUP_WITH_ELEMENT_WITH_MULTIPLE_STRINGS) => CONTENT_STRING_FOR_MULTIPLE_STRINGS)

(fact "Convert hiccup to content-string when an element is empty"  
      (diffs/hiccup->content-string HICCUP_WITH_EMPTY_ELEMENT) => CONTENT_STRING_FOR_EMPTY_ELEMENT)

(fact "Diff elements are removed"
      (diffs/remove-hiccup-elements DIFF_1 :ins) => '([:span nil "Equal text. "])
      (diffs/remove-hiccup-elements DIFF_2 :del) => '([:span nil "First paragraph."] [:span nil "Third paragraph."]))

(fact "Difference between drafts 2 and 3 is returned"
      (diffs/get-diffs-between-drafts {:content HICCUP_3} {:content HICCUP_2}) => {:previous-draft-diffs FORMATTED_PREVIOUS_DIFF_2_vs_3
                                                                                   :current-draft-diffs FORMATTED_CURRENT_DIFF_2_vs_3})

(fact "Difference between drafts 1 and 2 is returned"
      (diffs/get-diffs-between-drafts {:content HICCUP_2} {:content HICCUP_1}) => {:previous-draft-diffs FORMATTED_PREVIOUS_DIFF_1_vs_2
                                                                                   :current-draft-diffs FORMATTED_CURRENT_DIFF_1_vs_2})

(fact "Difference between hiccup with an element with multiple strings and hiccup with an empty element" 
      (diffs/get-diffs-between-drafts {:content HICCUP_WITH_EMPTY_ELEMENT} 
                                      {:content HICCUP_WITH_ELEMENT_WITH_MULTIPLE_STRINGS}) 
      => {:current-draft-diffs  '(["p"  {}]  ["p"  [:ins nil "barry"]])
          :previous-draft-diffs '(["p"  [:del nil "string one"]  [:del nil " string two"]  [:del nil " string three"]]  ["p"  [:del nil "another-string"]])})




(fact "Insert diffs into drafts 1"
      (diffs/insert-diffs-into-drafts '([:span nil "First paragraph."]) '(["p" {} "First paragraph."])) => '(["p" {} [:span nil "First paragraph."]])) 

(fact "Insert diffs into drafts 2"
      (diffs/insert-diffs-into-drafts '([:span nil "First"] [:ins nil " paragraph."]) '(["p" {} "First paragraph."])) => '(["p" {} [:span nil "First"] [:ins nil " paragraph."]]))

(fact "Insert diffs into drafts 3"
      (diffs/insert-diffs-into-drafts '([:span nil "First paragraph."]) '(["h1" {} "First"] ["p" {} " paragraph."])) => 
 '(["h1" {} [:span nil "First"]] ["p" {} [:span nil " paragraph."]]))

(fact "Insert diffs into drafts 4"
      (diffs/insert-diffs-into-drafts '([:span nil "First paragraph."] [:del nil "List item 1."] [:span nil "List item 2."]) '(["h1" {} "First paragraph."] ["ul" {} ["li" {} "List item 1."] ["li" {} "List item 2."]] )) => 
 '(["h1" {} [:span nil "First paragraph."]] ["ul" {} ["li" {} [:del nil "List item 1."]] ["li" {} [:span nil "List item 2."]]] ) )

(fact "Diff for n chars 1"
      (diffs/replacement-diff-for-n-chars 3 '([:span nil "Hello"] [:ins nil " world"])) => 
      {:replacement '([:span nil "Hel"])
       :updated-diff '([:span nil "lo"] [:ins nil " world"]) })

(fact "Diff for n chars 2"
      (diffs/replacement-diff-for-n-chars 7 '([:span nil "Hello"] [:ins nil " world"])) => 
      {:replacement '([:span nil "Hello"] [:ins nil " w"])
       :updated-diff '([:ins nil "orld"])})

(fact "Get replacement element and updated diff 1"
      (diffs/replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First"])) => 
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '()})
(fact "Get replacement element and updated diff 2"
      (diffs/replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First"] [:span nil " more text"])) => 
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '([:span nil " more text"])})

(fact "Get replacement element and updated diff 3"
      (diffs/replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First more text"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '([:span nil " more text"])})

(fact "Get replacement element and updated diff 4"
      (diffs/replacement-element-and-updated-diff ["h1" {} "First element"] '([:span nil "First"] [:ins nil " element"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"] [:ins nil " element"]]
                                                       :updated-diff '()}) 

(fact "Get replacement element and updated diff 5"
      (diffs/replacement-element-and-updated-diff ["h1" {} "First element"] '([:span nil "First"] [:ins nil " element"] [:span nil " more stuff"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"] [:ins nil " element"]]
                                                       :updated-diff '([:span nil " more stuff"])})
