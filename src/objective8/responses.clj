(ns objective8.responses
  (:refer-clojure :exclude [comment])
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.translation :refer [translation-config]]
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

;SHARING
(html/defsnippet share-widget
  "templates/share-widget.html"
  [:.share-widget] [translation uri title]
  [:.share-widget html/any-node] (html/replace-vars translation)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" (str utils/host-url uri) "t=" title " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" (str utils/host-url uri)))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" (str utils/host-url uri) "&text=" title " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" (str utils/host-url uri)))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" (str utils/host-url uri) "&title=" title " - "))
  [:.share-this-url-input] (html/set-attr :value (str utils/host-url uri)))

;NAVIGATION
(defn objectives-nav-selected-id [uri]
  (cond 
    (re-matches #".*/questions$" uri) :#clj-objectives-questions
    (re-matches #".*/objectives/\d+$" uri) :#clj-objectives-details))

(html/defsnippet objectives-navigation
  "templates/objectives-nav.html" [[:#navigation]] [objective translation uri]
  [:#clj-objective-title] (html/html-content (:title objective))
  [:#clj-objectives-details] (html/remove-class "selected")
  [(objectives-nav-selected-id uri)] (html/add-class "selected")
  [:#clj-objectives-details] (html/set-attr :href (str "/objectives/" (:_id objective)))
  [:#clj-objectives-questions] (html/set-attr :href (str "/objectives/" (:_id objective) "/questions"))
  [:.navigation-list] (html/after (share-widget translation uri (:title objective)))
  [:#navigation html/any-node] (html/replace-vars translation))

(html/defsnippet user-navigation-signed-in
  "templates/user-navigation/signed-in.html" [[:#clj-user-navigation]] [{:keys [translation]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translation))

(html/defsnippet user-navigation-signed-out
  "templates/user-navigation/signed-out.html" [[:#clj-user-navigation]] [{:keys [translation]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translation))

;BASE TEMPLATE
(html/deftemplate base
  "templates/base.html" [{:keys [translation locale doc-title doc-description user-navigation flash-message content objective uri]}]
  [:html] (html/set-attr :lang locale)
  ; TODO find a way to select description without an ID
  ; [:head (html/attr= :name "description")] (html/set-attr :content "some text")
  [:title] (html/content doc-title)
  [:#clj-description] (html/set-attr :content doc-description)
  [:.masthead] (html/append user-navigation)
  [:.browserupgrade] (html/html-content (translation :base/browsehappy))
  [:.header-logo] (html/content (translation :base/header-logo-text))
  [:.header-logo] (html/set-attr :title (translation :base/header-logo-title))
  [:#projectStatus] (html/html-content (translation :base/project-status))
  [:#main-content] (html/before (if flash-message (flash-message-view flash-message)))
  [:#main-content] (html/content content)
  [:#clj-navigation] (if objective (html/content (objectives-navigation objective translation uri)) identity)
  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))

;GUIDANCE
(html/defsnippet guidance
  "templates/big-guidance.html" [[:.grid-container]] [])

;HOME/INDEX
(html/defsnippet index-page
  "templates/index.html" [[:#clj-index]] [{:keys [translation signed-in]}]
  [:.index-get-started] (if signed-in (html/html-content (translation :index/index-get-started-signed-in)) (html/html-content (translation :index/index-get-started-signed-out)))
  [:.index-get-started] (if signed-in (html/set-attr :title (translation :index/index-get-started-title-signed-in)) (html/set-attr :title (translation :index/index-get-started-title-signed-out)))
  [:#clj-index html/any-node] (html/replace-vars translation)
  [:.index-intro] (html/after (guidance)))

;SIGN IN
(html/defsnippet sign-in-twitter
  "templates/sign-in-twitter.html" [[:#clj-sign-in-twitter]] [])

(html/defsnippet sign-in-page
  "templates/sign-in.html" [[:#clj-sign-in-page]] [{:keys [translation]}]
  [:h1] (html/after (sign-in-twitter))
  [:#clj-sign-in-page html/any-node] (html/replace-vars translation))

;PROJECT STATUS
(html/defsnippet project-status-page
  "templates/project-status.html" [[:#clj-project-status]] [{:keys [translation]}]
  [:#clj-project-status html/any-node] (html/replace-vars translation)
  [:#clj-project-status-detail] (html/html-content (translation :project-status/page-content)))

;LEARN MORE
(html/defsnippet learn-more-page
  "templates/learn-more.html" [:#clj-learn-more] [{:keys [translation]}]
  [:#clj-learn-more html/any-node] (html/replace-vars translation)
  [:#clj-learn-more-detail] (html/html-content (translation :learn-more/page-content)))

;ERROR 404
(html/defsnippet error-404-page
  "templates/error-404.html" [:#clj-error-404] [{:keys [translation]}]
  [:#clj-error-404 html/any-node] (html/replace-vars translation)
  [:#clj-error-404-content] (html/html-content (translation :error-404/page-content)))

;INVITATIONS
(html/defsnippet invitation-create
  "templates/writers/invitation-form.html" [[:#clj-invitation]] [translation objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" objective-id "/writers/invitations"))
  [:#clj-invitation html/any-node] (html/replace-vars translation))

(html/defsnippet post-invitation-container 
  "templates/writers/invitation-create.html" [[:#clj-invitation-container]] [translation signed-in objective-id uri] 
  [:#clj-invitation-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-invitation-container :.response-form] (if signed-in (html/content (invitation-create translation objective-id)) identity)
  [:#clj-invitation-container html/any-node] (html/replace-vars translation))

(html/defsnippet invitation-response-page
  "templates/writers/invitation-response.html" [:#clj-invitation-response]
  [{:keys [translation objective uri signed-in]}]
  [:#clj-invitation-response-sign-in] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-invitation-response-sign-in] (when-not signed-in identity)
  [:#clj-invitation-response-accept] (html/set-attr :action (str "/objectives/" (:_id objective) "/writers/invitation/accept"))
  [:#clj-invitation-response-accept] (when signed-in identity)
  [:#clj-invitation-response-objective-title] (html/content (:title objective))
  [:#clj-invitation-response html/any-node] (html/replace-vars translation))

;CANDIDATES

(html/defsnippet a-candidate
  "templates/writers/a-candidate.html" [:li] [candidate]
  [:.candidate-name] (html/content (:name candidate))
  [:.candidate-reason] (html/content (text->p-nodes (:invitation-reason candidate)))
  [:.invited-by] (html/append " user-display-name"))

(html/defsnippet candidate-list-page
  "templates/writers/candidate-list.html" [:#clj-candidate-list-container] [{:keys [translation objective signed-in uri candidates]}]
  [:#objective-crumb] (html/set-attr :title (:title objective))
  [:#objective-crumb] (html/content (:title objective))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (:_id objective)))
  [:#candidates-crumb] (html/set-attr :href (str "/objectives/" (:_id objective) "/candidate-writers"))
  [:#clj-candidate-list-container :h1] (html/content (:title objective))
  [:#clj-candidate-list] (if (empty? candidates) identity (html/content (map a-candidate candidates)))
  [:#clj-candidate-list-container] (html/after (post-invitation-container translation signed-in (:_id objective) uri))
  [:#clj-candidate-list-container html/any-node] (html/replace-vars translation))

;ANSWERS
(html/defsnippet answer-create
  "templates/answers/answer-create.html" [[:#clj-answer-create]] [translation objective-id question-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" objective-id "/questions/" question-id "/answers"))
  [:#clj-answer-create html/any-node] (html/replace-vars translation))

(html/defsnippet an-answer
  "templates/answers/answer.html" [:li] [answer]
  [:.answer-text] (html/content (text->p-nodes (:answer answer)))
  [:.answer-author] (html/content "user-display-name")
  [:.answer-date] (html/content (utils/iso-time-string->pretty-time (:_created_at answer))))

(html/defsnippet post-answer-container
  "templates/answers/post-answer-container.html" [[:#clj-post-answer-container]] [translation signed-in objective-id question-id uri]
  [:#clj-answer-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-post-answer-container :.response-form] (if signed-in (html/content (answer-create translation objective-id question-id)) identity )
  [:#clj-post-answer-container html/any-node] (html/replace-vars translation))

;QUESTIONS
(html/defsnippet a-question
  "templates/questions/a-question.html" [:li] [question]
  [:#clj-question-uri] (html/set-attr :href (str "/objectives/" (:objective-id question) "/questions/" (:_id question)))
  [:.question-text] (html/content (text->p-nodes (:question question)))
  [:.question-author] (html/content "user-display-name")
  [:.question-date] (html/content (utils/iso-time-string->pretty-time (:_created_at question)))) 
  
(html/defsnippet question-create
  "templates/questions/question-create.html" [:#clj-question-create] [translation objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" objective-id "/questions"))
  [:#clj-question-create html/any-node] (html/replace-vars translation))

(html/defsnippet post-question-container
  "templates/questions/post-question-container.html" [:#clj-post-question-container] [translation signed-in objective-id uri]
  [:#clj-question-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-post-question-container :.response-form] (if signed-in (html/content (question-create translation objective-id)) identity)
  [:#clj-post-question-container html/any-node] (html/replace-vars translation))

(html/defsnippet question-list-page
  "templates/questions/question-list.html" [:#clj-question-list] [{:keys [translation objective questions signed-in uri]}]
  [:#objective-crumb] (html/set-attr :title (:title objective))
  [:#objective-crumb] (html/content (:title objective))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (:_id objective)))
  [:#questions-crumb] (html/set-attr :href (str "/objectives/" (:_id objective) "/questions"))
  [:#clj-question-list :h1] (html/content (:title objective))
  [:#clj-question-list :.question-list] (if (empty? questions) identity (html/content (map a-question questions)))
  [:#clj-question-list] (html/after (post-question-container translation signed-in (:_id objective) uri))
  [:#clj-question-list html/any-node] (html/replace-vars translation))

(html/defsnippet question-view-page
  "templates/questions/question-view.html" [:#clj-question-view] [{:keys [translation objective question answers signed-in uri]}]
  [:#objective-crumb] (html/set-attr :title (:title objective))
  [:#objective-crumb] (html/content (:title objective))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (:objective-id question)))
  [:#questions-crumb] (html/set-attr :href (str "/objectives/" (:objective-id question) "/questions"))
  [:#question-crumb] (html/set-attr :href (str "/objectives/" (:objective-id question) "/questions/" (:_id question)))
  [:#question-crumb] (html/set-attr :title (:question question))
  [:#question-crumb] (html/content (:question question))
  [:#clj-question-view :h1] (html/content (:question question))
  [:#clj-question-view :.answer-list] (if (empty? answers) identity (html/content (map an-answer answers)))
  [:#clj-question-view] (html/after (post-answer-container translation signed-in (:objective-id question) (:_id question) uri))
  [:#clj-question-view html/any-node] (html/replace-vars translation))

;COMMENTS
(html/defsnippet comment-create
  "templates/comments/comment-create.html" [[:#clj-comment-create]] [objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#objective-id] (html/set-attr :value objective-id))

(html/defsnippet a-comment
  "templates/comments/comment.html" [:li] [comment]
  [:.comment-text] (html/content (text->p-nodes (:comment comment)))
  [:.comment-author] (html/content "user-display-name")
  [:.comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment))))

(html/defsnippet comments-view
  "templates/comments/comments-view.html" [[:#clj-comments-view]] [translation signed-in objective-id comments uri]
  [:#clj-comment-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-comments-view :.response-form] (if signed-in (html/content (comment-create objective-id)) identity )
  [:#clj-comments-view html/any-node] (html/replace-vars translation)
  [:#clj-comments-view :.comment-list] (if (empty? comments) identity (html/content (map a-comment comments))))

;OBJECTIVES
(html/defsnippet a-goal
  "templates/goal.html" [:li] [goal]
  [:li] (html/content goal))

(defn shorten-content [content]
  (let [content (or content "")
        shortened-content (clojure.string/trim (subs content 0 (min (count content) 100)))]
    (when-not (empty? shortened-content)
      (str shortened-content "..."))))

(html/defsnippet objective-list-entry {:parser jsoup/parser}
  "templates/objectives-list.html" [:.clj-objectives-list-entry] [objective]
  [:.clj-objective-title] (html/content (:title objective))
  [:.clj-objective-brief-description] (html/content (shorten-content (:description objective)))
  [:.clj-objective-end-date] (html/content (:end-date objective))
  [:.clj-objective-link] (html/set-attr :href (str "/objectives/" (:_id objective))))

(html/defsnippet objective-list-page
  "templates/objectives-list.html" [[:#clj-objectives-list]] [{:keys [translation objectives]}]
  [:ol] (html/content (map objective-list-entry objectives))
  [:#clj-objectives-list html/any-node] (html/replace-vars translation))

(html/defsnippet objective-create-page
  "templates/objectives-create.html" [[:#clj-objective-create]] [{:keys [translation]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-objective-create html/any-node] (html/replace-vars translation))

(html/defsnippet objective-detail-page
  "templates/objectives-detail.html" [[:#clj-objectives-detail]]
  [{:keys [translation objective signed-in comments uri]}]
  [:#clj-objectives-detail html/any-node] (html/replace-vars translation)
  [:h1] (html/content (:title objective))
  [:#clj-obj-goals-value] (html/content (map a-goal (:goals objective)))
  [:#clj-obj-background-label] (if (empty? (:description objective)) nil identity)
  [:#clj-obj-background-label] (html/after (text->p-nodes (:description objective)))
  [:#clj-obj-end-date-value] (html/content (:end-date objective))
  [:#clj-objectives-detail] (html/after (comments-view translation signed-in (:_id objective) comments uri)))


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
  (let [user-navigation (if (:signed-in args)
                             user-navigation-signed-in
                             user-navigation-signed-out)
        page (render-template base (assoc args
                                          :content (template-name args)
                                          :flash-message (:message args)
                                          :user-navigation (user-navigation args)))]
        (if-let [status-code (:status-code args)]
          (simple-response page status-code)
          (simple-response page))))
