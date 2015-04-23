(ns objective8.draft-diffs-new
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

(defn has-content? [element]
  (or (> (count element) 2)
      (sequential? (last element))
      (string? (last element))))

(defn remove-empty-elements [hiccup]
  (filter has-content? hiccup))

(defn remove-tags [hiccup-draft]
  (->> hiccup-draft
       remove-empty-elements
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


(defn wrap-with-tag [elements tag]
  (apply merge [tag] elements))

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


(defn format-diff [{:keys [draft-tag-types diff-char-count draft-char-count formatted-elements diffs] :as data}]
 (let [paragraph-size (first draft-char-count) 
     ;  paragraph-size (min (first (:draft-char-count data)) (reduce + diff-char-count))
       cumulative-sum (reductions + diff-char-count)
       index (count (filter #(< % paragraph-size) cumulative-sum))
       elements-to-format (get-elements-to-format diffs index cumulative-sum paragraph-size)]
   {:formatted-elements (into formatted-elements [(wrap-with-tag elements-to-format (first draft-tag-types))])
    :draft-tag-types (rest draft-tag-types)
    :diff-char-count (get-remaining-char-count diff-char-count index cumulative-sum paragraph-size)
    :draft-char-count (drop 1 (:draft-char-count data))
    :diffs (get-remaining-diffs diffs index cumulative-sum paragraph-size)}))


(defn get-types-for-element [element]
  (let [element-content (get-element-content element)]
    (if (string? (first element-content))
      (first element)
      (map get-types-for-element element-content))))

(defn get-types-for-hiccup [text]
  (-> (map get-types-for-element text)
      flatten))

(defn get-char-count-for-element [element]
  (let [element-content (get-element-content element)]
    (if (string? (first element-content)) 
      (count (first element-content))
      (map get-char-count-for-element element-content))))

(defn get-char-counts-for-hiccup [text]
  (->> (map get-char-count-for-element text)
       (remove #(= 0 %))
       flatten))

(defn replacement-diff-for-n-chars [n diff]
  (let [diff-element (first diff)
        diff-element-string (last diff-element) 
        diff-element-char-count (count diff-element-string)]
    (if (<= n diff-element-char-count)
      (let [extracted-diff-element (assoc diff-element 2 (subs diff-element-string 0 n )) 
            remaining-diff-element-string (subs diff-element-string n)
            updated-diff-element (if (empty? remaining-diff-element-string)
                                   '() 
                                   (list (assoc diff-element 2 remaining-diff-element-string)))]
        {:replacement (list extracted-diff-element) 
         :updated-diff (concat updated-diff-element (rest diff)) })
      (let [recursive-result (replacement-diff-for-n-chars (- n diff-element-char-count) (rest diff))
            recursive-replacement (:replacement recursive-result)
            recursive-updated-diff (:updated-diff recursive-result)]
        {:replacement (concat (list diff-element) recursive-replacement)
         :updated-diff recursive-updated-diff}))))

(defn get-replacement-element-and-updated-diff [element diff] 
   (let [element-char-count (count (last element))
         {replacement :replacement updated-diff :updated-diff} (replacement-diff-for-n-chars element-char-count diff)] 
     {:replacement-element  (concat (pop element) replacement)
      :updated-diff updated-diff})) 

  #_(let [element-char-count (count (last element))
        diff-element (first diff)
        diff-element-string (last diff-element) 
        diff-element-char-count (count diff-element-string)] 
    (if (> element-char-count diff-element-char-count)
      {:replacement (concat (assoc element 2 diff-element) (rest diff))
       :updated-diff '()}
      (let [replacement-string (subs diff-element-string 0 element-char-count) 
            remaining-diff-element-string (subs diff-element-string element-char-count) 
            extracted-diff-element (assoc diff-element 2 replacement-string) 
            updated-diff-element (if (empty? remaining-diff-element-string)
                                   '() 
                                   (list ( assoc diff-element 2 remaining-diff-element-string)))
            _ (prn element-char-count) 
            _ (prn diff-element) 
            _ (prn diff-element-string) 
            _ (prn replacement-string) 
            _ (prn remaining-diff-element-string)  
            _ (prn extracted-diff-element)   
            _ (prn updated-diff-element)]   
        {:replacement (assoc element 2 extracted-diff-element)  
         :updated-diff (concat updated-diff-element (rest diff))}))) 

(defn insert-diffs-into-drafts [diffs draft]
  (let [first-draft-element (first draft)
        returned-draft (list (concat (subvec first-draft-element 0 (- (count first-draft-element) 1)) diffs))
        ]
    returned-draft
    )
  )

(defn add-formatting [diffs draft]
  (prn "IN ADD FORMATTING")
  (prn "DIFFS" diffs)
  (prn "DRAFT" draft)
  (def formatted-draft (atom {:formatted-elements []  
                              :draft-tag-types (get-types-for-hiccup draft)
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
        _ (prn "CURRENT DRAFT CONTENT" current-draft-content)
        previous-draft-content (apply list (:content previous-draft))
        _ (prn "PREVIOUS DRAFT CONTENT" previous-draft-content)
        diffs (diff-hiccup-content previous-draft-content current-draft-content)
        _ (prn "DIFFS" diffs) 
        previous-draft-diffs (remove-hiccup-elements diffs :ins)
        _ (prn "PREVIOUS DRAFT DIFFS" previous-draft-diffs) 
        current-draft-diffs (remove-hiccup-elements diffs :del)
        _ (prn "CURRENT DRAFT DIFFS" current-draft-diffs) 
        result {:previous-draft-diffs (add-formatting previous-draft-diffs previous-draft-content)
                :current-draft-diffs (add-formatting current-draft-diffs current-draft-content)}
        _ (prn "RESULT" result) ]
    result))
