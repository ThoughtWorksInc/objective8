(ns d-cent.responses
  (:require [net.cgrand.enlive-html :as html]
            [d-cent.translation :refer [translation-config]]
            [d-cent.config :as config]))

(def google-analytics-tracking-id (config/get-var "GA_TRACKING_ID"))

;GOOGLE ANALYTICS
(html/defsnippet google-analytics "templates/google-analytics.html" [[:#clj-google-analytics]]
                                                                    [tracking-id]
                                                                  (html/transform-content (html/replace-vars {:trackingID tracking-id})))

;BASE TEMPLATE
(html/deftemplate base "templates/base.html"
                  [{:keys [translation
                           locale
                           doc-title
                           doc-description
                           global-navigation
                           content]}]
                  [:html] (html/set-attr :lang locale)
                  ; TODO find a way to select description without an ID
                  ; [:head (html/attr= :name "description")] (html/set-attr :content "some text")
                  [:title] (html/content doc-title)
                  [:#clj-description] (html/set-attr :content doc-description)
                  [:#clj-global-navigation] (html/content global-navigation)
                  [:.browserupgrade] (html/html-content (translation :base/browsehappy))
                  [:.header-logo] (html/content (translation :base/header-logo-text))
                  [:.header-logo] (html/set-attr :title (translation :base/header-logo-title))
                  [:#main-content] (html/content content)
                  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))


;NAVIGATION
(html/defsnippet global-navigation-signed-in "templates/navigation-global-signed-in.html" [[:.global-navigation]]
                                                                                          [{:keys [translation]}]
                                                                                            [:#clj-sign-out-link] (html/content (translation :navigation-global/sign-out-text))
                                                                                            [:#clj-sign-out-link] (html/set-attr :title (translation :navigation-global/sign-out-title))
                                                                                            [:#clj-user-profile-link] (html/content (translation :navigation-global/profile-text))
                                                                                            [:#clj-user-profile-link] (html/set-attr :title (translation :navigation-global/profile-title)))

(html/defsnippet global-navigation-signed-out "templates/navigation-global-signed-out.html" [[:.global-navigation]]
                                                                                            [{:keys [translation]}]
                                                                                            [:#clj-sign-in-link] (html/content (translation :navigation-global/sign-in-text))
                                                                                            [:#clj-sign-in-link] (html/set-attr :title (translation :navigation-global/sign-in-title)))
;HOME/INDEX
(html/defsnippet index-page "templates/index.html" [[:#clj-index]]
                                                    [{:keys [translation]}]
                                                    [:.index-welcome] (html/content (translation :index/index-welcome))
                                                    [:.index-intro] (html/content (translation :index/index-intro))
                                                    [:.index-get-started] (html/content (translation :index/index-get-started))
                                                    [:.index-get-started] (html/set-attr :title (translation :index/index-get-started-title))
                                                    [:.index-learn-more] (html/content (translation :index/index-learn-more))
                                                    [:.index-learn-more] (html/set-attr :title (translation :index/index-learn-more-title)))

;SIGN IN
(html/defsnippet sign-in-page "templates/sign-in.html" [[:#clj-sign-in-page]]
                                                        [{:keys [translation]}]
                                                        [:h1] (html/content (translation :sign-in/page-title))
                                                        [:#clj-sign-in-twitter] (html/set-attr :value (translation :sign-in/twitter-sign-in-btn))
                                                        [:#clj-sign-in-twitter] (html/set-attr :title (translation :sign-in/twitter-sign-in-title)))


;OBJECTIVES
(html/defsnippet objective-create-page "templates/objectives-create.html" [[:#clj-objective-create]]
                                                                          [{:keys [translation]}]
                                                                          [:h1] (html/content (translation :objective-create/page-title))
                                                                          [(html/attr= :for "objective-title")] (html/content (translation :objective-create/title-label))
                                                                          [(html/attr= :name "title")] (html/set-attr :title (translation :objective-create/title-title))
                                                                          [(html/attr= :for "objective-goals")] (html/content (translation :objective-create/goals-label))
                                                                          [(html/attr= :name "goals")] (html/set-attr :title (translation :objective-create/goals-title))
                                                                          [(html/attr= :for "objective-description")] (html/content (translation :objective-create/description-label))
                                                                          [(html/attr= :name "description")] (html/set-attr :title (translation :objective-create/description-title))
                                                                          [(html/attr= :for "objective-end-date")] (html/content (translation :objective-create/end-date-label))
                                                                          [(html/attr= :name "end-date")] (html/set-attr :title (translation :objective-create/end-date-title))
                                                                          [:.button] (html/content (translation :objective-create/submit)))

(html/defsnippet objective-view-page "templates/objectives-view.html" [[:#clj-objectives-view]]
                                                                       [{:keys [translation objective]}]
                                                                       [:h1] (html/content (:title objective))
                                                                       [:#clj-obj-goals-label] (html/content (translation :objective-view/goals-label))
                                                                       [:#clj-obj-goals-value] (html/content (:goals objective))
                                                                       [:#clj-obj-description-label] (html/content (translation :objective-view/description-label))
                                                                       [:#clj-obj-description-value] (html/content (:description objective))
                                                                       [:#clj-obj-date-label] (html/content (translation :objective-view/end-date-label))
                                                                       [:#clj-obj-end-date-value] (html/content (:end-date objective)))

;USERS
(html/defsnippet users-email "templates/users-email.html" [[:#clj-users-email]]
                                                           [{:keys [translation]}]
                                                           [:h1] (html/content (translation :users-email/page-title))
                                                           [(html/attr= :for "email-address")] (html/content (translation :users-email/email-label))
                                                           [(html/attr= :name "email-address")] (html/set-attr :title (translation :users-email/email-title))
                                                           [:button] (html/content (translation :users-email/button)))

(defn render-template [template & args]
  (apply str (apply template args)))

(defn simple-response
  "Returns a response with given status code or 200"
  ([text]
   (simple-response text 200))
  ([text status-code]
   {:status status-code
    :header {"Content-Type" "text/html"}
    :body text}))

(defn rendered-response [template-name args]
  (let [navigation (if (:signed-in args)
                         global-navigation-signed-in
                         global-navigation-signed-out)
        page (render-template base (assoc args
                                          :content (template-name args)
                                          :global-navigation (navigation args)))]
        (if-let [status-code (:status-code args)]
          (simple-response page status-code)
          (simple-response page))))
