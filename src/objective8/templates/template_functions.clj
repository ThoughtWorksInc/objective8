(ns objective8.templates.template-functions
  (:require [net.cgrand.enlive-html :as html]))


(defn translator
  "Returns a translation function which replaces the
  content of nodes with translations for k"
  [{:keys [translations] :as context}]
  (fn [k] 
    #(assoc % :content (translations k))))

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
  Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
      (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                             newline-followed-by-optional-whitespace)))))

(defn- translate-node  [node  {:keys  [translations] :as context}]
  (let [[translation-type-string translation-key-string] (clojure.string/split (get-in node [:attrs :data-l8n]) #":") 
        translation-type (keyword translation-type-string)
        translation-key (keyword translation-key-string)
        translation (translations translation-key)]
    (cond 
      (= :content translation-type) 
      (assoc node :content (list translation))  
      
      (= :html translation-type) 
      (assoc node :content (html/html-snippet translation))

      (= "attr" (namespace translation-type))
      (assoc-in node [:attrs (keyword (name translation-type))] translation)

      :else node)))


(defn translate [context nodes]
  (html/at nodes
           [(html/attr? :data-l8n)] #(translate-node % context)))
