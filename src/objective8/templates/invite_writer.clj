(ns objective8.templates.invite-writer
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def objective-invite-writer-template (html/html-resource "templates/jade/objective-invite-writer.html" {:parser jsoup/parser}))

(defn invite-writer-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (html/at objective-invite-writer-template
                      [:title] (html/content (:title doc))
                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-navigation-item-objective] (html/set-attr "href" (str "/objectives/" (:_id objective)))
                      [:.l8n-back-to-objective] (html/content (translations :objective-nav/back-to-objective))
                      [:.clj-objective-title] (html/content (:title objective))
                      [:.l8n-invite-writer-section-title] (html/content (translations :invite-writer/section-title))
                      [:.l8n-do-you-know-a-writer] (html/content (translations :invite-writer/do-you-know-a-writer))

                      [:.clj-invite-a-writer-form] (html/content (f/invite-writer context))
                      [:.clj-invite-a-writer-form] (html/set-attr "action" (str "/objectives/" (:_id objective) "/writer-invitations"))

                      )))))
