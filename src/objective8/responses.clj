(ns objective8.responses
  (:refer-clojure :exclude [comment])
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.config :as config]
            [objective8.utils :as utils]))

(def google-analytics-tracking-id (config/get-var "GA_TRACKING_ID"))

(defn objective-url [objective]
  (str utils/host-url "/objectives/" (:_id objective)))

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

;GOOGLE ANALYTICS
(html/defsnippet google-analytics
  "templates/google-analytics.html" [[:#clj-google-analytics]] [tracking-id]
  (html/transform-content (html/replace-vars {:trackingID tracking-id})))

;FLASH MESSAGES
(html/defsnippet flash-message-view
  "templates/flash-message.html" [[:#clj-flash-message]] [message]
  [:p] (html/html-content message))

(defn generate-invitation-link [invitation]
  (str utils/host-url "/objectives/" (:objective-id invitation) "/writer-invitations/" (:invitation-id invitation)))

;INVITATION BANNER
(html/defsnippet invitation-banner
  "templates/invitation-banner.html" [[:#clj-invitation-banner]] [invitation translations]
  [:a] (html/set-attr :href (generate-invitation-link invitation))
  [:#clj-invitation-banner html/any-node] (html/replace-vars translations))

;SHARING
(html/defsnippet share-widget
  "templates/share-widget.html"
  [:.share-widget] [{:keys [translations data ring-request] :as context}]
  [:.share-widget html/any-node] (html/replace-vars translations)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" (str utils/host-url (:uri ring-request)) "t=" (get-in data [:objective :title]) " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" (str utils/host-url (:uri ring-request))))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" (str utils/host-url (:uri ring-request)) "&text=" (get-in data [:objective :title]) " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" (str utils/host-url (:uri ring-request))))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" (str utils/host-url (:uri ring-request)) "&title=" (get-in data [:objective :title]) " - "))
  [:.share-this-url-input] (html/set-attr :value (str utils/host-url (:uri ring-request))))

;NAVIGATION
(defn objectives-nav-selected-id [uri]
  (cond 
    (re-matches #".*/questions$" uri) :#clj-objectives-questions
    (re-matches #".*/objectives/\d+$" uri) :#clj-objectives-details
    (re-matches #".*/candidate-writers$" uri) :#clj-objectives-writers))

(html/defsnippet objectives-navigation
  "templates/objectives-nav.html" [[:#navigation]] [{:keys [translations data ring-request] :as context}]
  [:#clj-objective-title] (html/html-content (get-in data [:objective :title]))
  [:#clj-objectives-details] (html/remove-class "selected")
  [(objectives-nav-selected-id (:uri ring-request))] (html/add-class "selected")
  [:#clj-objectives-details] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id])))
  [:#clj-objectives-questions] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/questions"))
  [:#clj-objectives-writers] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/candidate-writers"))
  [:.navigation-list] (html/after (share-widget context))
  [:#navigation html/any-node] (html/replace-vars translations))

(html/defsnippet user-navigation-signed-in
  "templates/user-navigation/signed-in.html" [[:#clj-user-navigation]] [{:keys [translations user]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translations)
  [:#clj-username] (html/content (:username user)))

(html/defsnippet user-navigation-signed-out
  "templates/user-navigation/signed-out.html" [[:#clj-user-navigation]] [{:keys [translations ring-request] :as context}]
  [:a] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-user-navigation html/any-node] (html/replace-vars translations))

;BASE TEMPLATE
(html/deftemplate base
  "templates/base.html" [{:keys [translations doc user-navigation invitation-rsvp content data] :as context}]
  ; TODO find a way to select description without an ID
  ; [:head (html/attr= :name "description")] (html/set-attr :content "some text")
  [:title] (html/content (doc :title))
  [:#clj-description] (html/set-attr :content (doc :description))
  [:.masthead] (html/append user-navigation)
  [:.browserupgrade] (html/html-content (translations :base/browsehappy))
  [:.header-logo] (html/content (translations :base/header-logo-text))
  [:.header-logo] (html/set-attr :title (translations :base/header-logo-title))
  [:#projectStatus] (html/html-content (translations :base/project-status))
  [:#main-content] (html/before (when-let [flash-message (:flash doc)] (flash-message-view flash-message)))
  [:#main-content] (html/before (when invitation-rsvp (invitation-banner invitation-rsvp translations)))
  [:#main-content] (html/content content)
  [:#clj-navigation] (if (:objective data) (html/content (objectives-navigation context)) identity)
  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))

;GUIDANCE
(html/defsnippet guidance
  "templates/big-guidance.html" [[:.grid-container]] [])

;;ARTICLE META
(html/defsnippet article-meta
  "templates/article-meta.html" [:div.article-meta] [objective translations]
  [:div.article-meta html/any-node] (html/replace-vars translations)
  [:.clj-objective-drafting-message] (when (:drafting-started objective) identity)
  [:.clj-objective-drafting-start-date-message] (when-not (:drafting-started objective) identity)
  [:.clj-objective-drafting-link] (html/set-attr :href (str "/objectives/" (:_id objective) "/drafts/latest"))
  [:#clj-obj-end-date-value] (html/content (:end-date objective)))

;PROJECT STATUS
(html/defsnippet project-status-page
  "templates/project-status.html" [[:#clj-project-status]] [{:keys [translations]}]
  [:#clj-project-status html/any-node] (html/replace-vars translations)
  [:#clj-project-status-detail] (html/html-content (translations :project-status/page-content)))

;ERROR 404
(html/defsnippet error-404-page
  "templates/error-404.html" [:#clj-error-404] [{:keys [translations]}]
  [:#clj-error-404 html/any-node] (html/replace-vars translations)
  [:#clj-error-404-content] (html/html-content (translations :error-404/page-content)))

;INVITATIONS
(html/defsnippet invitation-create
  "templates/writers/invitation-form.html" [[:#clj-invitation]] [{:keys [translations data] :as context}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" (get-in data [:objective :_id]) "/writer-invitations"))
  [:#clj-invitation html/any-node] (html/replace-vars translations))

(html/defsnippet post-invitation-container 
  "templates/writers/invitation-create.html" [[:#clj-invitation-container]] [{:keys [translations user ring-request data] :as context}] 
  [:#clj-invitation-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-invitation-container :.response-form] (if user (html/content (invitation-create context)) identity)
  [:#clj-invitation-container html/any-node] (html/replace-vars translations))

;ANSWERS
(html/defsnippet answer-create
  "templates/answers/answer-create.html" [[:#clj-answer-create]] [{:keys [translations data] :as context}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" (get-in data [:objective :_id]) "/questions/" (get-in data [:question :_id]) "/answers"))
  [:#clj-answer-create html/any-node] (html/replace-vars translations))

(html/defsnippet an-answer
  "templates/answers/answer.html" [:li] [answer]
  [:.answer-text] (html/content (text->p-nodes (:answer answer)))
  [:.answer-author] (html/content (:username answer))
  [:.answer-date] (html/content (utils/iso-time-string->pretty-time (:_created_at answer))))


;COMMENTS

(html/defsnippet comment-create
  "templates/comments/comment-create.html" [[:#clj-comment-create]] [objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#objective-id] (html/set-attr :value objective-id))

(html/defsnippet a-comment
  "templates/comments/comment.html" [:li] [comment]
  [:.comment-text] (html/content (text->p-nodes (:comment comment)))
  [:.comment-author] (html/content (:username comment))
  [:.comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment))))

(html/defsnippet comments-view
  "templates/comments/comments-view.html" [[:#clj-comments-view]] [{:keys [translations ring-request user data] :as context}]
  [:#clj-comment-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-comments-view :.response-form] (if user (html/content (comment-create (get-in data [:objective :_id]))) identity )
  [:#clj-comments-view html/any-node] (html/replace-vars translations)
  [:#clj-comments-view :.comment-list] (let [comments (:comments data)] (if (empty? comments) identity (html/content (map a-comment comments)))))

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

(defn rendered-response [template-name context]
  (let [user-navigation (if (:user context)
                          user-navigation-signed-in
                          user-navigation-signed-out)
        ;TODO - simplify this to just pass through context
        page (render-template base (assoc context
                                          :content (template-name context)
                                          :user-navigation (user-navigation context)))]
    (if-let [status-code (:status-code context)]
      (simple-response page status-code)
      (simple-response page))))
