(ns objective8.responses
  (:refer-clojure :exclude [comment])
  (:require [net.cgrand.enlive-html :as html]
            [objective8.translation :refer [translation-config]]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def google-analytics-tracking-id (config/get-var "GA_TRACKING_ID"))

(defn objective-url [objective]
  (str utils/host-url "/objectives/" (:_id objective)))

;GOOGLE ANALYTICS
(html/defsnippet google-analytics
  "templates/google-analytics.html" [[:#clj-google-analytics]] [tracking-id]
  (html/transform-content (html/replace-vars {:trackingID tracking-id})))

;FLASH MESSAGES
(html/defsnippet flash-message-view
  "templates/flash-message.html" [[:#clj-flash-message]] [message]
  [:p] (html/html-content message))

;BASE TEMPLATE
(html/deftemplate base
  "templates/base.html" [{:keys [translation locale doc-title doc-description global-navigation flash-message content]}]
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
  [:.global-navigation html/any-node] (html/replace-vars translation))

(html/defsnippet global-navigation-signed-out
  "templates/navigation-global-signed-out.html" [[:.global-navigation]] [{:keys [translation]}]
  [:.global-navigation html/any-node] (html/replace-vars translation))

;HOME/INDEX
(html/defsnippet index-page
  "templates/index.html" [[:#clj-index]] [{:keys [translation signed-in]}]
  [:.index-get-started] (if signed-in (html/html-content (translation :index/index-get-started-signed-in)) (html/html-content (translation :index/index-get-started-signed-out)))
  [:.index-get-started] (if signed-in (html/set-attr :title (translation :index/index-get-started-title-signed-in)) (html/set-attr :title (translation :index/index-get-started-title-signed-out)))
  [:#clj-index html/any-node] (html/replace-vars translation))

;SIGN IN
(html/defsnippet sign-in-twitter
  "templates/sign-in-twitter.html" [[:#clj-sign-in-twitter]] [])

(html/defsnippet sign-in-page
  "templates/sign-in.html" [[:#clj-sign-in-page]] [{:keys [translation]}]
  [:#clj-sign-in-page] (html/append (sign-in-twitter))
  [:#clj-sign-in-page html/any-node] (html/replace-vars translation))

;COMMENTS
(html/defsnippet comment-create
  "templates/comment-create.html" [[:#clj-comment-create]] [objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#objective-id] (html/set-attr :value objective-id))

(html/defsnippet comment-sign-in
  "templates/comment-sign-in.html" [[:#clj-comment-sign-in]] [])

(html/defsnippet a-comment
  "templates/comment.html" [:li] [comment]
  [:li] (html/content (:comment comment)))

(html/defsnippet comments-view
  "templates/comments-view.html" [[:#clj-comments-view]] [translation signed-in objective-id comments]
  [:#clj-comments-view] (html/append (if signed-in (comment-create objective-id) (comment-sign-in)))
  [:#clj-comments-view html/any-node] (html/replace-vars translation)
  [:#clj-comments-view :.comment-list] (html/content (map a-comment comments)))

;OBJECTIVES
(html/defsnippet objective-create-page
  "templates/objectives-create.html" [[:#clj-objective-create]] [{:keys [translation]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-objective-create html/any-node] (html/replace-vars translation))

(html/defsnippet objective-view-page
  "templates/objectives-view.html" [[:#clj-objectives-view]]
  [{:keys [translation objective signed-in comments]}]
  [:.objective-article-details html/any-node] (html/replace-vars translation)
  [:h1] (html/content (:title objective))
  [:#clj-obj-goals-value] (html/content (:goals objective))
  [:#clj-obj-background-value] (html/content (:description objective))
  [:#clj-obj-end-date-value] (html/content (:end-date objective))
  [:.share-widget html/any-node] (html/replace-vars translation)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" (objective-url objective) "t=" (:title objective) " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" (objective-url objective)))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" (objective-url objective) "&text=" (:title objective) " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" (objective-url objective)))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" (objective-url objective) "&title=" (:title objective) " - "))
  [:.share-this-url] (html/set-attr :value (objective-url objective))
  [:.share-widget] (html/after (comments-view translation signed-in (:_id objective) comments)))

;USERS
(html/defsnippet users-email
  "templates/users-email.html" [[:#clj-users-email]] [{:keys [translation]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-users-email html/any-node] (html/replace-vars translation))


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
