(ns objective8.templates.objective-list
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.permissions :as permissions]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))   

(def objective-list-resource (html/html-resource "templates/jade/objective-list.html" {:parser jsoup/parser}))

(def objective-list-item-resource (html/select pf/library-html-resource [:.clj-objective-list-item]))
(def objective-list-item-removal-container (html/select objective-list-resource 
                                                        [:.clj-objective-list-item-removal-container]))

(defn removal-container [{:keys [title uri] :as objective}]
  (html/at objective-list-item-removal-container
           [:.clj-objective-removal-form] (html/prepend (html/html-snippet (anti-forgery-field)))
           [:.clj-removal-uri] (html/set-attr :value uri)
           [:.clj-removal-sample] (html/set-attr :value title)))

(defn- shorten-content [content]
  (let [content (or content "")
        shortened-content (clojure.string/trim (subs content 0 (min (count content) 100)))]
    (when-not (empty? shortened-content)
      (str shortened-content "..."))))

(defn brief-description [objective]
  (shorten-content (:description objective)))

(defn objective-list-items [{:keys [translations data user] :as context}]
  (let [objectives (:objectives data)]
    (html/at objective-list-item-resource [:.clj-objective-list-item] 
             (html/clone-for [objective objectives]
                             [:.clj-objective-list-item-star] (if (get-in objective [:meta :starred])
                                                                (html/add-class "starred")
                                                                identity)    

                             [:.clj-objective-list-item-removal-container] (when (permissions/admin? user) 
                                                                             (html/substitute (removal-container objective)))

                             [:.clj-objective-list-item-link] (html/set-attr :href (str "/objectives/" 
                                                                                        (:_id objective)))
                             [:.clj-objective-list-item-title] (html/content (:title objective))

                             [:.l8n-drafting-begins] 
                             (html/content (if (tf/in-drafting? objective)
                                             (translations :objective-list/drafting-started)
                                             (translations :objective-list/drafting-begins)))

                             [:.clj-objective-drafting-begins-date] 
                             (when (tf/open? objective) 
                               (html/do->
                                 (html/set-attr :drafting-begins-date (:end-date objective)) 
                                 (html/content (str (:days-until-drafting-begins objective)
                                                    " " 
                                                    (translations :objective-list/days))))) 

                             [:.clj-objective-brief-description]
                             (html/content (brief-description objective))
                             ))))

(defn objective-list-page [{:keys [translations data doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at objective-list-resource
                                    [:title] (html/content (:title doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                    [:.clj-guidance-buttons] nil
                                    [:.l8n-guidance-heading] (html/content (translations :objectives-guidance/heading))

                                    [:.clj-objective-list] (html/content (objective-list-items context))))))))
