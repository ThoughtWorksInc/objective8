(ns objective8.drafts
  (:require [crypto.random :as random] 
            [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn uri-for-draft [{:keys [_id objective-id] :as draft}]
  (str "/objectives/" objective-id "/drafts/" _id))

(defn insert-section-label [element label]
  (let [{:keys [element-without-content content]} (utils/split-hiccup-element element)
        section-label-attr {:data-section-label label}]
    (if (empty? content)
      element 
      (case (count element-without-content)
        1 (concat element-without-content [section-label-attr] content)
        2 (assoc element 1 (merge (second element) section-label-attr))))))

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

(defn retrieve-latest-draft [objective-id]
  (when-let [latest-draft (-> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                                                   {:sort {:field :_created_at :ordering :DESC}})
                              :result
                              first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft latest-draft))]
      (-> latest-draft
          (dissoc :_created_at_sql_time :global-id)
          (utils/update-in-self [:uri] uri-for-draft)
          (assoc :previous-draft-id previous-draft-id)
          ))))

(defn retrieve-drafts [objective-id]
  (->> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:limit 50
                            :sort {:field :_created_at :ordering :DESC}})
       :result
       (map #(dissoc % :created_at_sql_time :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-draft))))

(defn get-section-labels-for-draft [draft-id]
  (let [{:keys [content] :as draft} (retrieve-draft draft-id)]
    (map get-section-label content)))

