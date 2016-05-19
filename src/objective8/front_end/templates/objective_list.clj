(ns objective8.front-end.templates.objective-list
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.utils :as utils]))

(def objective-list-resource (html/html-resource "templates/jade/objective-list.html" {:parser jsoup/parser}))

(def objective-list-item-with-promote-resource (html/select pf/library-html-resource [:.clj-library-key--objective-list-item-with-promote-form]))
(def objective-list-item-with-demote-resource (html/select pf/library-html-resource [:.clj-library-key--objective-list-item-with-demote-form]))


(def objective-list-item-removal-container (html/select objective-list-item-with-promote-resource
                                                        [:.clj-objective-list-item-removal-container]))
(def objective-list-item-promotion-container (html/select objective-list-item-with-promote-resource
                                                          [:.clj-promote-objective-form-container]))
(def objective-list-item-demotion-container (html/select objective-list-item-with-demote-resource
                                                          [:.clj-promote-objective-form-container]))

(def MAX_PROMOTED_OBJECTIVES 3)

(def promoted true)

(defn removal-container [anti-forgery-snippet {:keys [title uri] :as objective}]
  (html/at objective-list-item-removal-container
           [:.clj-objective-removal-form] (html/prepend anti-forgery-snippet)
           [:.clj-removal-uri] (html/set-attr :value uri)
           [:.clj-removal-sample] (html/set-attr :value title)))

(defn promotion-container [anti-forgery-snippet {:keys [uri promoted] :as objective}]
  (html/at (if promoted objective-list-item-demotion-container objective-list-item-promotion-container)
           [:.clj-promote-objective-form] (html/prepend anti-forgery-snippet)
           [:.clj-promotion-uri] (html/set-attr :value uri)
           ))

(defn- shorten-content [content]
  (let [content (or content "")
        shortened-content (clojure.string/trim (subs content 0 (min (count content) 100)))]
    (when-not (empty? shortened-content)
      (str shortened-content "..."))))

(defn brief-description [objective]
  (shorten-content (:description objective)))

(defn promoted-objective? [objective] (true? (:promoted objective)))

(defn max-promoted-objectives? [data]
  (let [promoted-objectives (filter #(= (promoted-objective? %) true) (:objectives data))]
    (<= MAX_PROMOTED_OBJECTIVES (count promoted-objectives))))

(defn no-promoted-objectives? [{:keys [data] :as context}]
  (let [objectives (filter #(= (promoted-objective? %) true) (:objectives data))]
    (empty? objectives)))

(defn objective-list-items [{:keys [anti-forgery-snippet data user] :as context} promoted]
  (let [objectives (filter #(= (promoted-objective? %) promoted) (:objectives data))]
    (html/at objective-list-item-with-promote-resource [:.clj-objective-list-item]
             (html/clone-for [{objective-id :_id :as objective} objectives]
                             [:.clj-objective-list-item-star] (if (get-in objective [:meta :starred])
                                                                (html/add-class "starred")
                                                                identity)

                             [:.clj-objective-list-item-removal-container] (when (permissions/admin? user)
                                                                             (html/substitute (removal-container anti-forgery-snippet objective)))

                             [:.clj-objective-list-dashboard-link] (when (permissions/writer-for? user objective-id)
                                                                     (html/set-attr :href (utils/path-for :fe/dashboard-questions :id objective-id)))

                             [:.clj-promote-objective-form-container] (when (and (permissions/admin? user) (or promoted (not (max-promoted-objectives? data))))
                                                                        (html/substitute (promotion-container anti-forgery-snippet objective)))

                             [:.clj-objective-list-item-link] (html/set-attr :href (str "/objectives/"
                                                                                        objective-id))
                             [:.clj-objective-list-item-title] (html/content (:title objective))
                             [:.clj-objective-brief-description] (html/content (brief-description objective))))))

(defn objective-list-page [{:keys [translations user doc] :as context}]
  (html/at objective-list-resource
           [:title] (html/content (:title doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

           [:.clj-guidance-buttons] nil
           [:.l8n-guidance-heading] (html/content (translations :objectives-guidance/heading))
           [:.clj-promoted-objective-information] (if (not (permissions/admin? user)) (html/substitute nil) identity)

           [:.clj-objective-list] (html/content (objective-list-items context (not promoted)))
           [:.clj-promoted-objective-list] (html/content (objective-list-items context promoted))
           [:.clj-promoted-objectives-container] (if (no-promoted-objectives? context) (html/substitute nil) identity)))
