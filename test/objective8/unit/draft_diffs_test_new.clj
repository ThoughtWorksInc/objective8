(ns objective8.unit.draft-diffs-test-new
  (:require [midje.sweet :refer :all]
            [objective8.draft-diffs-new :as diffs]))

(def HICCUP_1 '(["p" {} "First paragraph."] ["ul" {} ["li" {} "List item 1."] ["li" {} "List item 2."] ["li" {} "List item 3."]] ["p" {} "Last paragraph."]))
(def HICCUP_2 '(["p" {} "First paragraph."] ["p" {} "Second paragraph."] ["p" {} "Third paragraph."]))
(def HICCUP_3 '(["p" {} "First paragraph."] ["p" {} "New paragraph."] ["p" {} "Third paragraph."]))

(def ELEMENT [:del {} "First sentence. Second sentence."]) 

(def DIFF_1 '([:span nil "Equal text. "] [:ins nil "New text."]))
(def DIFF_2 '([:span nil "First paragraph."] [:del nil "Second paragraph."] [:span nil "Third paragraph."]))

(def FORMATTED_DIFF_1 '([:p [:span nil "Equal text. "] [:ins nil "New text."]])) 
(def FORMATTED_DIFF_2 '(["p" [:span nil "First paragraph."]] ["p" [:del nil "Second paragraph."]] ["p" [:span nil "Third paragraph."]]))
(def FORMATTED_PREVIOUS_DIFF_2_vs_3 '(["p" [:span nil "First paragraph."]] ["p" [:del nil "Second"] [:span " paragraph."]] ["p" [:span "Third paragraph."]]))
(def FORMATTED_CURRENT_DIFF_2_vs_3 '(["p" [:span nil "First paragraph."]] ["p" [:ins nil "New"] [:span " paragraph."]] ["p" [:span "Third paragraph."]]))
(def FORMATTED_PREVIOUS_DIFF_1_vs_2 '(["p" [:span nil "First paragraph."]] ["ul" ["li" [:del nil "List item 1."]] ["li" [:del nil "List item 2."]] ["li" [:del nil "List item 3."]]] ["p" [:del "Last"] [:span " paragraph."]]))
(def FORMATTED_CURRENT_DIFF_1_vs_2 '(["p" [:span nil "First paragraph."]]  ["p" [:ins "Second paragraph."]] ["p" [:ins "Third"] [:span nil " paragraph."]]))

(fact "Tags are removed from a simple hiccup"
      (diffs/remove-tags HICCUP_2) => "First paragraph.Second paragraph.Third paragraph.")

(fact "Tags are removed from hiccup"
      (diffs/remove-tags HICCUP_1) => "First paragraph.List item 1.List item 2.List item 3.Last paragraph.")

(fact "Characters in the hiccup element are counted"
      (diffs/get-char-count-for-element ELEMENT) => 32)

(fact "Characters in hiccup are counted"
      (diffs/get-char-counts-for-hiccup HICCUP_1) => '(16 12 12 12 15))

(fact "Hiccup element is divided at character position"
      (diffs/split-element-at-position ELEMENT 16) => [[:del "First sentence. "] [:del "Second sentence."]])

(fact "Diff elements are removed"
      (diffs/remove-hiccup-elements DIFF_1 :ins) => '([:span nil "Equal text. "])
      (diffs/remove-hiccup-elements DIFF_2 :del) => '([:span nil "First paragraph."] [:span nil "Third paragraph."]))

(fact "Diff elements are wrapped in paragraph tags"
      (diffs/format-diff {:formatted-elements []
                          :draft-tag-types '(:p)
                          :diff-char-count '(12 9)
                          :draft-char-count '(21)
                          :diffs DIFF_1}) => {:formatted-elements FORMATTED_DIFF_1
                                              :draft-tag-types '()
                                              :diff-char-count '()
                                              :draft-char-count '()
                                              :diffs '()})

(fact "Diffs are formatted into a draft"
      (diffs/add-formatting DIFF_2 HICCUP_2) => FORMATTED_DIFF_2)

(fact "Difference between drafts 2 and 3 is returned"
      (diffs/get-diffs-between-drafts {:content HICCUP_3} {:content HICCUP_2}) => {:previous-draft-diffs FORMATTED_PREVIOUS_DIFF_2_vs_3
                                                                                   :current-draft-diffs FORMATTED_CURRENT_DIFF_2_vs_3})

(future-fact "Difference between drafts 1 and 2 is returned"
      (diffs/get-diffs-between-drafts {:content HICCUP_2} {:content HICCUP_1}) => {:previous-draft-diffs FORMATTED_PREVIOUS_DIFF_1_vs_2
                                                                                   :current-draft-diffs FORMATTED_CURRENT_DIFF_1_vs_2})

(fact "Diff tag types are extracted"
      (diffs/get-types-for-hiccup HICCUP_1) => ["p" "li" "li" "li" "p"])

(fact "Insert diffs into drafts 1"
      (diffs/insert-diffs-into-drafts '([:span nil "First paragraph."]) '(["p" {} "First paragraph."])) => '(["p" {} [:span nil "First paragraph."]])) 

(fact "Insert diffs into drafts 2"
      (diffs/insert-diffs-into-drafts '([:span nil "First"] [:ins nil " paragraph."]) '(["p" {} "First paragraph."])) => '(["p" {} [:span nil "First"] [:ins nil " paragraph."]]))

(future-fact "Insert diffs into drafts 3"
      (diffs/insert-diffs-into-drafts '([:span nil "First paragraph."]) '(["h1" {} "First"] ["p" {} " paragraph."])) => 
 '(["h1" {} [:span nil "First"]] ["p" {} [:span nil " paragraph."]]))

(fact "Diff for n chars 1"
      (diffs/replacement-diff-for-n-chars 3 '([:span nil "Hello"] [:ins nil " world"])) => 
      {:replacement '([:span nil "Hel"])
       :updated-diff '([:span nil "lo"] [:ins nil " world"]) })

(fact "Diff for n chars 2"
      (diffs/replacement-diff-for-n-chars 7 '([:span nil "Hello"] [:ins nil " world"])) => 
      {:replacement '([:span nil "Hello"] [:ins nil " w"])
       :updated-diff '([:ins nil "orld"])})

(fact "Get replacement element and updated diff 1"
      (diffs/get-replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First"])) => 
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '()})
(fact "Get replacement element and updated diff 2"
      (diffs/get-replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First"] [:span nil " more text"])) => 
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '([:span nil " more text"])})

(fact "Get replacement element and updated diff 3"
      (diffs/get-replacement-element-and-updated-diff ["h1" {} "First"] '([:span nil "First more text"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"]]
                                                       :updated-diff '([:span nil " more text"])})

(fact "Get replacement element and updated diff 4"
      (diffs/get-replacement-element-and-updated-diff ["h1" {} "First element"] '([:span nil "First"] [:ins nil " element"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"] [:ins nil " element"]]
                                                       :updated-diff '()}) 

(fact "Get replacement element and updated diff 5"
      (diffs/get-replacement-element-and-updated-diff ["h1" {} "First element"] '([:span nil "First"] [:ins nil " element"] [:span nil " more stuff"])) =>
                                                      {:replacement-element ["h1" {} [:span nil "First"] [:ins nil " element"]]
                                                       :updated-diff '([:span nil " more stuff"])}) 
