(ns objective8.back-end.domain.drafts
  (:require [crypto.random :as random] 
            [objective8.back-end.storage.storage :as storage] 
            [objective8.back-end.storage.uris :as uris]
            [objective8.utils :as utils]))

(defn uri-for-draft [{:keys [_id objective-id] :as draft}]
  (str "/objectives/" objective-id "/drafts/" _id))

(defn uri-for-section [{:keys [objective-id draft-id section-label] :as section}]
  (str "/objectives/" objective-id "/drafts/" draft-id "/sections/" section-label))

(defn insert-section-label [element label]
  (let [{:keys [element-without-content content]} (utils/split-hiccup-element element)
        section-label-attr {:data-section-label label}]
    (if (empty? content)
      element 
      (case (count element-without-content)
        1 (into [] (concat element-without-content [section-label-attr] content)) 
        2 (into [] (assoc element 1 (merge (second element) section-label-attr)))))))

(defn generate-section-label []
  (random/hex 4))

(defn get-n-unique-section-labels [n]
  (take n (distinct (repeatedly generate-section-label)))) 

(defn add-section-labels [hiccup]
  (let [number-of-sections (count hiccup)
        section-labels (get-n-unique-section-labels number-of-sections)]
  (into [] (map insert-section-label hiccup section-labels))))

(defn get-section-label [element]
  (when (map? (second element))
    (:data-section-label (second element))))

(defn store-section! [section-data]
  (-> (assoc section-data :entity :section)
      storage/pg-store!))

(defn store-draft! [draft-data]
  (some-> draft-data
          (update-in [:content] add-section-labels)
          (assoc :entity :draft)
          storage/pg-store!
          (utils/update-in-self [:uri] uri-for-draft)
          (dissoc :global-id)))

(defn retrieve-drafts [objective-id]
  (->> (storage/pg-get-drafts objective-id)
       (map #(dissoc % :created_at_sql_time :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-draft))))

(defn retrieve-previous-draft [draft]
  (-> (storage/pg-retrieve {:entity :draft
                            :objective-id (:objective-id draft) 
                            :_created_at ['< (:_created_at_sql_time draft)]})
      :result
      last))

(defn retrieve-next-draft [draft]
  (-> (storage/pg-retrieve {:entity :draft
                            :objective-id (:objective-id draft)
                            :_created_at ['> (:_created_at_sql_time draft)]})
      :result
      first))

(defn retrieve-draft [draft-id]
  (when-let [draft (-> (storage/pg-retrieve {:entity :draft 
                                             :_id draft-id})
                       :result
                       first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft draft))
          next-draft-id (:_id (retrieve-next-draft draft))]
      (-> draft
          (dissoc :_created_at_sql_time :global-id)
          (utils/update-in-self [:uri] uri-for-draft)
          (assoc :previous-draft-id previous-draft-id :next-draft-id next-draft-id)))))

(defn retrieve-draft-with-comment-count [draft-id]
  (when-let [draft (-> (storage/pg-get-draft-with-comment-count draft-id)
                       first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft draft))
          next-draft-id (:_id (retrieve-next-draft draft))]
      (-> draft
          (dissoc :_created_at_sql_time :global-id)
          (utils/update-in-self [:uri] uri-for-draft)
          (assoc :previous-draft-id previous-draft-id :next-draft-id next-draft-id)))))

(defn retrieve-draft-by-uri [draft-uri]
  (when-let [draft (-> (storage/pg-retrieve (uris/uri->query draft-uri))
                       :result
                       first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft draft))
          next-draft-id (:_id (retrieve-next-draft draft))]
      (-> draft
          (dissoc :_created_at_sql_time :global-id)
          (utils/update-in-self [:uri] uri-for-draft)
          (assoc :previous-draft-id previous-draft-id :next-draft-id next-draft-id)))))

(defn retrieve-latest-draft [objective-id]
  (when-let [latest-draft (-> (storage/pg-get-drafts objective-id)
                              first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft latest-draft))]
      (-> latest-draft
          (dissoc :_created_at_sql_time :global-id)
          (utils/update-in-self [:uri] uri-for-draft)
          (assoc :previous-draft-id previous-draft-id)))))

(defn get-ordered-section-labels-for-draft-hiccup [hiccup]
  (map get-section-label hiccup))

(defn get-section-labels-for-draft-uri [draft-uri]
  (let [{:keys [content] :as draft} (retrieve-draft-by-uri draft-uri)]
    (get-ordered-section-labels-for-draft-hiccup content)))

(defn has-section-label? [section-label section]
  (some-> (second section)
      :data-section-label
      (= section-label)))

(defn get-section-from-hiccup [hiccup section-label] 
  (some #(when (has-section-label? section-label %)  [%]) hiccup))

(defn get-section [section-uri]
  (let [{:keys [entity section-label draft-id] :as query} (uris/uri->query section-uri)]
    (when (= :section entity)
      (let [{:keys [content objective-id] :as draft} (retrieve-draft draft-id)
            section (get-section-from-hiccup content section-label)]
        (when section
          {:section section
           :uri section-uri
           :objective-id objective-id})))))

(defn get-section-label-from-uri [section-uri]
  (:section-label (uris/uri->section-data section-uri)))

(defn merge-section-content-with-section [hiccup section]
  (assoc section :section (get-section-from-hiccup hiccup (-> (:uri section)
                                                              get-section-label-from-uri))))
(defn retrieve-annotated-sections [sections-uri]
  (->> (storage/pg-retrieve (uris/uri->query sections-uri))
       :result
       (map #(dissoc % :global-id :_id :_created_at :entity)) 
       (map #(utils/update-in-self % [:uri] uri-for-section))  
       (map #(dissoc % :draft-id :section-label))))

(defn get-annotated-sections-with-section-content [draft-uri]
  (let [sections-uri (str draft-uri "/sections")
        annotated-sections (retrieve-annotated-sections sections-uri)
        draft-content (:content (retrieve-draft-by-uri draft-uri))
        ordered-section-labels (get-ordered-section-labels-for-draft-hiccup draft-content)
        ordered-annotated-sections (sort-by 
                                     #((into {} (map-indexed (fn [i e] [e i]) ordered-section-labels)) 
                                       (get-section-label-from-uri (:uri %))) 
                                     annotated-sections)]
    (map #(merge-section-content-with-section draft-content %) ordered-annotated-sections)))

(defn get-draft-sections-with-annotation-count [draft-uri]
  (let [{:keys [entity _id objective-id] :as query} (uris/uri->query draft-uri)]
    (when (= :draft entity)
      (->> (storage/pg-get-draft-sections-with-annotation-count _id objective-id)
           (map #(dissoc % :global-id :_id :_created_at :entity))
           (map #(utils/update-in-self % [:uri] uri-for-section))))))
