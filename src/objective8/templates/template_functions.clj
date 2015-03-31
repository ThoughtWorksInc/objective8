(ns objective8.templates.template-functions)


(defn translator
  "Returns a translation function which replaces the
   content of nodes with translations for k"
  [{:keys [translations] :as context}]
  (fn [k] 
    #(assoc % :content (translations k))))

