(ns d-cent.responses
  (:require [net.cgrand.enlive-html :as html]
            [d-cent.translation :refer [translation-config]]
            [d-cent.config :as config]
            [d-cent.utils :as utils]))

(def google-analytics-tracking-id (config/get-var "GA_TRACKING_ID"))
(defn objective-url [objective]
  (str utils/host-url "/objectives/" (:_id objective)))

;GOOGLE ANALYTICS
(html/defsnippet google-analytics
  "templates/google-analytics.html" [[:#clj-google-analytics]] [tracking-id]
  (html/transform-content (html/replace-vars {:trackingID tracking-id})))

;FLASH MESSAGES
(html/defsnippet flash-message-view "templates/flash-message.html" [[:#clj-flash-message]]
                  [message]
                    [:p] (html/html-content message))


;BASE TEMPLATE
(html/deftemplate base "templates/base.html"
                  [{:keys [translation
                           locale
                           doc-title
                           doc-description
                           global-navigation
                           flash-message
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
                  [:.page-container] (html/before (if flash-message (flash-message-view flash-message)))
                  [:#main-content] (html/content content)
                  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))

;NAVIGATION
(html/defsnippet global-navigation-signed-in
  "templates/navigation-global-signed-in.html" [[:.global-navigation]] [{:keys [translation]}]
  [:#clj-sign-out-link] (html/content (translation :navigation-global/sign-out-text))
  [:#clj-sign-out-link-title] (html/set-attr :title (translation :navigation-global/sign-out-title))
  [:#clj-user-profile-link] (html/content (translation :navigation-global/profile-text))
  [:#clj-user-profile-link] (html/set-attr :title (translation :navigation-global/profile-title)))

(html/defsnippet global-navigation-signed-out
  "templates/navigation-global-signed-out.html" [[:.global-navigation]] [{:keys [translation]}]
  [:#clj-sign-in-link] (html/content (translation :navigation-global/sign-in-text))
  [:#clj-sign-in-link-title] (html/set-attr :title (translation :navigation-global/sign-in-title)))

;HOME/INDEX
(html/defsnippet index-page
  "templates/index.html" [[:#clj-index]] [{:keys [translation]}]
  [:.index-welcome] (html/content (translation :index/index-welcome))
  [:.index-intro] (html/content (translation :index/index-intro))
  [:.index-get-started] (html/content (translation :index/index-get-started))
  [:.index-get-started] (html/set-attr :title (translation :index/index-get-started-title))
  [:.index-learn-more] (html/content (translation :index/index-learn-more))
  [:.index-learn-more] (html/set-attr :title (translation :index/index-learn-more-title)))

;SIGN IN
(html/defsnippet sign-in-page
  "templates/sign-in.html" [[:#clj-sign-in-page]] [{:keys [translation]}]
  [:h1] (html/content (translation :sign-in/page-title))
  [:#clj-sign-in-twitter] (html/set-attr :value (translation :sign-in/twitter-sign-in-btn))
  [:#clj-sign-in-twitter] (html/set-attr :title (translation :sign-in/twitter-sign-in-title)))


;OBJECTIVES
(html/defsnippet objective-create-page
  "templates/objectives-create.html" [[:#clj-objective-create]] [{:keys [translation]}]
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

(html/defsnippet objective-view-page
  "templates/objectives-view.html" [[:#clj-objectives-view]]
  [{:keys [translation objective]}]
  [:h1] (html/content (:title objective))
  [:#clj-obj-goals-label] (html/content (translation :objective-view/goals-label))
  [:#clj-obj-goals-value] (html/content (:goals objective))
  [:#clj-obj-description-label] (html/content (translation :objective-view/description-label))
  [:#clj-obj-description-value] (html/content (:description objective))
  [:#clj-obj-date-label] (html/content (translation :objective-view/end-date-label))
  [:#clj-obj-end-date-value] (html/content (:end-date objective))
  [:.share-widget html/any-node] (html/replace-vars translation)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" (objective-url objective) "t=" (:title objective) " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" (objective-url objective)))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" (objective-url objective) "&text=" (:title objective) " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" (objective-url objective)))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" (objective-url objective) "&title=" (:title objective) " - "))
  [:.share-this-url] (html/set-attr :value (objective-url objective)))

;USERS
(html/defsnippet users-email
  "templates/users-email.html" [[:#clj-users-email]] [{:keys [translation]}]
  [:h1] (html/content (translation :users-email/page-title))
  [:.clj-user-email-welcome] (html/content (translation :users-email/user-email-welcome))
  [(html/attr= :for "email-address")] (html/content (translation :users-email/email-label))
  [(html/attr= :name "email-address")] (html/set-attr :title (translation :users-email/email-title))
  [:button] (html/content (translation :users-email/button))
  [:.clj-users-email-continue] (html/content (translation :users-email/continue))
  [:.clj-users-email-continue] (html/set-attr :title (translation :users-email/continue-title)))

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
                                          :flash-message (:message args)
                                          :global-navigation (navigation args)))]
        (if-let [status-code (:status-code args)]
          (simple-response page status-code)
          (simple-response page))))
