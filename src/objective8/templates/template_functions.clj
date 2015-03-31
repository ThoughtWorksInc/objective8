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

