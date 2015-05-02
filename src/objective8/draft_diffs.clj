(ns objective8.draft-diffs
  (:require [diff-match-patch-clj.core :as dmp]
            [objective8.utils :as utils]
            [objective8.drafts :as drafts]))

(declare replacement-element-and-updated-diff strings-for-element)

(defn replacement-diff-for-n-chars [n diff]
  (if (zero? n) 
    {:replacement (list "")
     :updated-diff diff}
    (let [diff-element (first diff)
          diff-element-string (last diff-element) 
          diff-element-char-count (count diff-element-string)]
      (if (<= n diff-element-char-count)
        (let [extracted-diff-element (conj (pop diff-element) (subs diff-element-string 0 n)) 
              remaining-diff-element-string (subs diff-element-string n)
              updated-diff-element (if (empty? remaining-diff-element-string)
                                     '() 
                                     (list (conj (pop diff-element) remaining-diff-element-string)))]
          {:replacement (list extracted-diff-element) 
           :updated-diff (concat updated-diff-element (rest diff)) })
        (let [recursive-result (replacement-diff-for-n-chars (- n diff-element-char-count) (rest diff))
              recursive-replacement (:replacement recursive-result)
              recursive-updated-diff (:updated-diff recursive-result)]
          {:replacement (into [] (concat (list diff-element) recursive-replacement)) 
           :updated-diff recursive-updated-diff})))))

(defn replacement-content-and-updated-diff [content diff]
  ;; Each thing in content will be a string or a vector (another html element)
  (if (empty? content)
    {:replacement-content content
     :updated-diff diff} 
    (let [first-element (first content)]
      (if (string? first-element)
        (let [{:keys [replacement updated-diff]} (replacement-diff-for-n-chars (count first-element) diff)
              {recursive-replacement :replacement-content 
               recursive-updated-diff :updated-diff} (replacement-content-and-updated-diff (rest content) updated-diff)]
          {:replacement-content (into [] (concat replacement recursive-replacement)) 
           :updated-diff recursive-updated-diff})

        ;; else first-element must be a nested-tag vector
        (let [{:keys [replacement-element updated-diff]} (replacement-element-and-updated-diff first-element diff)
              {recursive-replacement :replacement-content 
               recursive-updated-diff :updated-diff} (replacement-content-and-updated-diff (rest content) updated-diff)] 
          {:replacement-content (into [] (concat [replacement-element] recursive-replacement))
           :updated-diff recursive-updated-diff})))))

(defn replacement-element-and-updated-diff [element diff] 
  (let [{:keys [element-without-content content]} (utils/split-hiccup-element element) 
        {:keys [replacement-content updated-diff]} (replacement-content-and-updated-diff content diff)]
    {:replacement-element (into [] (concat element-without-content replacement-content)) 
     :updated-diff updated-diff})) 

(defn insert-diffs-into-drafts [diffs draft]
  (if (empty? draft)
    draft
    (let [first-draft-element (first draft)
          {:keys [replacement-element updated-diff]} (replacement-element-and-updated-diff first-draft-element diffs)
          recursive-returned-draft (insert-diffs-into-drafts updated-diff (rest draft))]
      (concat (list replacement-element) recursive-returned-draft))))

(defn remove-hiccup-elements [hiccup element]
  (remove #(= element (first %)) hiccup))

(defn strings-for-element [element]
  ;; Element is either a string or a vector (a hiccup html element)
  (if (string? element)
    [element]
    (->> (utils/split-hiccup-element element)
         :content
         (mapcat strings-for-element))))

(defn hiccup->content-string [draft]
  (clojure.string/join (mapcat strings-for-element draft))) 

(defn diff-hiccup-content [hiccup-1 hiccup-2]
  (-> (dmp/diff (hiccup->content-string hiccup-1) (hiccup->content-string hiccup-2))
      dmp/cleanup!
      dmp/as-hiccup))

(defn get-diffs-between-drafts [draft previous-draft]
  (let [current-draft-content (utils/sanitise-hiccup (:content draft))
        previous-draft-content (utils/sanitise-hiccup (:content previous-draft))
        diffs (diff-hiccup-content previous-draft-content current-draft-content)
        previous-draft-diffs (remove-hiccup-elements diffs :ins)
        current-draft-diffs (remove-hiccup-elements diffs :del)]
    {:previous-draft-diffs (insert-diffs-into-drafts previous-draft-diffs previous-draft-content)
     :current-draft-diffs (insert-diffs-into-drafts current-draft-diffs current-draft-content)}))
