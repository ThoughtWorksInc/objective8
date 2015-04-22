(ns objective8.draft-diffs
  (:require [diff-match-patch-clj.core :as dmp]
            [objective8.utils :as utils]
            [objective8.drafts :as drafts]))


(defn hiccup->vector-of-strings [strings hcp] 
  (let [first-element (first hcp)] 
    (if (sequential? first-element) 
      (let [left-strings (hiccup->vector-of-strings strings first-element)]
        (if (next hcp)
          (hiccup->vector-of-strings left-strings (rest hcp)) 
          left-strings))
      (let [last-element (last hcp)]
        (if (string? last-element)
          (conj strings (last hcp))   
          (hiccup->vector-of-strings strings (rest hcp)))))))

(defn remove-tags [hiccup-draft]
  (->> hiccup-draft
       (hiccup->vector-of-strings [])
       clojure.string/join))

(defn remove-hiccup-elements [hiccup element]
  (filter #(not= element (first %)) hiccup))

(defn is-attribute? [data]
  (or (nil? data) (instance? clojure.lang.PersistentArrayMap data)))

(defn get-element-content [element]
 (if (is-attribute? (second element))
   (drop 2 element)
   (drop 1 element)))

(defn split-element-at-position [element char-position]
  (let [element-type (first element)
        element-content (first (get-element-content element))]
   [[element-type (subs element-content 0 char-position)] 
    [element-type (subs element-content char-position)]]))

(defn wrap-with-p-tag [elements]
  (apply merge [:p] elements))

(defn get-char-position [index paragraph-size cumulative-sum]
  (if (= 0 index)
    paragraph-size
    (- paragraph-size (nth cumulative-sum (dec index)))))

(defn get-elements-to-format [diffs index cumulative-sum paragraph-size]
  (let [complete-elements (take index diffs)
        char-position (get-char-position index paragraph-size cumulative-sum)
        final-element (if (> (nth cumulative-sum index) paragraph-size)
                       (first (split-element-at-position (nth diffs index) char-position)) 
                       (nth diffs index))]
    (concat complete-elements [final-element])))

(defn get-remaining-char-count [diff-char-count index cumulative-sum paragraph-size]
  (let [new-char-count (drop index diff-char-count)
        char-position (get-char-position index paragraph-size cumulative-sum)]
    (->> (rest new-char-count)
         (list* (- (first new-char-count) char-position)) 
         (remove #(= 0 %)))))

(defn get-remaining-diffs [diffs index cumulative-sum paragraph-size]
  (let [new-diffs (drop (inc index) diffs)
        char-position (if (= 0 index)
                        paragraph-size
                        (- paragraph-size (nth cumulative-sum (dec index))))]
    (if (> (nth cumulative-sum index) paragraph-size)
      (list* (second (split-element-at-position (nth diffs index) char-position)) new-diffs) 
      new-diffs)))

(defn format-diff [{:keys [diff-char-count draft-char-count formatted-elements diffs] :as data}]
 (let [paragraph-size (first draft-char-count) 
     ;  paragraph-size (min (first (:draft-char-count data)) (reduce + diff-char-count))
       cumulative-sum (reductions + diff-char-count)
       index (count (filter #(< % paragraph-size) cumulative-sum))
       elements-to-format (get-elements-to-format diffs index cumulative-sum paragraph-size)]
   {:formatted-elements (into formatted-elements [(wrap-with-p-tag elements-to-format)])
    :diff-char-count (get-remaining-char-count diff-char-count index cumulative-sum paragraph-size)
    :draft-char-count (drop 1 (:draft-char-count data))
    :diffs (get-remaining-diffs diffs index cumulative-sum paragraph-size)}))



(defn get-char-count-for-element [element]
  (let [element-content (get-element-content element)]
    (if (instance? String (first element-content)) 
      (count (first element-content))
      (map get-char-count-for-element element-content))))

(defn get-char-counts-for-hiccup [text]
  (->> (map get-char-count-for-element text)
       (remove #(= 0 %))
       flatten))

(defn add-formatting [diffs draft]
  (def formatted-draft (atom {:formatted-elements []  
                              :diff-char-count (get-char-counts-for-hiccup diffs)
                              :draft-char-count (get-char-counts-for-hiccup draft)
                              :diffs diffs}))
  (while (not-empty (:draft-char-count @formatted-draft))
    (swap! formatted-draft format-diff))
  (->> @formatted-draft
       :formatted-elements
       (into '())
       reverse))


(defn diff-hiccup-content [hiccup-1 hiccup-2]
  (-> (dmp/diff (remove-tags hiccup-1) (remove-tags hiccup-2))
      dmp/cleanup!
      dmp/as-hiccup))

(defn get-diffs-between-drafts [draft previous-draft]
  (let [current-draft-content (apply list (:content draft))
        previous-draft-content (apply list (:content previous-draft))
        diffs (diff-hiccup-content previous-draft-content current-draft-content)
        previous-draft-diffs (remove-hiccup-elements diffs :ins)
        current-draft-diffs (remove-hiccup-elements diffs :del)
        result {:previous-draft-diffs (add-formatting previous-draft-diffs previous-draft-content)
                :current-draft-diffs (add-formatting current-draft-diffs current-draft-content)}]
    result))
