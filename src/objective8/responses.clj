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
  "templates/user-navigation/signed-out.html" [[:#clj-user-navigation]] [{:keys [translations]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translations))

;BASE TEMPLATE
(html/deftemplate base
  "templates/base.html" [{:keys [translations doc user-navigation invitation content data] :as context}]
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
  [:#main-content] (html/before (when invitation (invitation-banner invitation translations)))
  [:#main-content] (html/content content)
  [:#clj-navigation] (if (:objective data) (html/content (objectives-navigation context)) identity)
  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))

;GUIDANCE
(html/defsnippet guidance
  "templates/big-guidance.html" [[:.grid-container]] [])

;HOME/INDEX
(html/defsnippet index-page
  "templates/index.html" [[:#clj-index]] [{:keys [translations]}]
  [:.index-get-started] (html/html-content (translations :index/index-get-started))
  [:.index-get-started] (html/set-attr :title (translations :index/index-get-started-title))
  [:#clj-index html/any-node] (html/replace-vars translations)
  [:.index-intro] (html/after (guidance)))

;SIGN IN
(html/defsnippet sign-in-twitter
  "templates/sign-in-twitter.html" [[:#clj-sign-in-twitter]] [])

(html/defsnippet sign-in-page
  "templates/sign-in.html" [[:#clj-sign-in-page]] [{:keys [translations]}]
  [:h1] (html/after (sign-in-twitter))
  [:#clj-sign-in-page html/any-node] (html/replace-vars translations))

;;ARTICLE META
(html/defsnippet article-meta
  "templates/article-meta.html" [:div.article-meta] [objective translations]
  [:div.article-meta html/any-node] (html/replace-vars translations)
  [:.clj-objective-drafting-message] (when (:drafting-started objective) identity)
  [:.clj-objective-drafting-start-date-message] (when-not (:drafting-started objective) identity)
  [:.clj-objective-drafting-link] (html/set-attr :href (str "/objectives/" (:_id objective) "/drafts")) 
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

;DRAFTS

(html/defsnippet current-draft-page
  "templates/drafts/current-draft.html" [:#clj-current-draft] [{:keys [translation objective signed-in uri]}]
  [:#objective-crumb] (html/set-attr :title (:title objective))
  [:#objective-crumb] (html/content (:title objective))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (:_id objective)))
  [:#drafts-crumb] (html/set-attr :href (str "/objectives/" (:_id objective) "/drafts"))
  [:h1] (html/content (:title objective)) 
  [:#clj-current-draft html/any-node] (html/replace-vars translation))

(html/defsnippet edit-draft-page
  "templates/drafts/edit-draft.html" [:#clj-edit-draft] [{:keys [translations data]}]
  [:#clj-edit-draft-preview] (some-> data :preview html/html-content)
  [:#clj-edit-draft-form] (html/set-attr :action (str "/objectives/" (:objective-id data) "/edit-draft"))
  [:#clj-edit-draft html/any-node] (html/replace-vars translations))

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

(html/defsnippet invitation-response-page
  "templates/writers/invitation-response.html" [:#clj-invitation-response]
  [{:keys [translations data user ring-request invitation] :as context}]
  [:#clj-invitation-response-sign-in-link] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-invitation-response-sign-in] (when-not user identity)
  [:#clj-invitation-response-accept] (html/set-attr :action (str "/objectives/" (get-in data [:objective :_id]) "/writer-invitations/" (:invitation-id invitation) "/accept"))
  [:#clj-invitation-response-accept] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-invitation-response-accept] (when user identity)
  [:#clj-invitation-response-decline] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-invitation-response-decline] (html/set-attr :action (str "/objectives/" (get-in data [:objective :_id]) "/writer-invitations/" (:invitation-id invitation) "/decline"))
  [:#clj-invitation-response-objective-title] (html/content (get-in data [:objective :title]))
  [:#clj-invitation-response html/any-node] (html/replace-vars translations))

;CANDIDATES

(html/defsnippet a-candidate
  "templates/writers/a-candidate.html" [:li] [candidate]
  [:.candidate-name] (html/content (:writer-name candidate))
  [:.candidate-reason] (html/content (text->p-nodes (:invitation-reason candidate))))

(html/defsnippet candidate-list-page
  "templates/writers/candidate-list.html" [:#clj-candidate-list-container] [{:keys [translations objective signed-in uri candidates data] :as context}]
  [:#objective-crumb] (html/set-attr :title (get-in data [:objective :title]))
  [:#objective-crumb] (html/content (get-in data [:objective :title]))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id])))
  [:#candidates-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/candidate-writers"))
  [:#clj-candidate-list-container :h1] (html/content (get-in data [:objective :title]))
  [:h1] (html/after (article-meta (:objective data) translations))
  [:#clj-candidate-list] (let [candidates (:candidates data)] (if (empty? candidates) identity (html/content (map a-candidate candidates)))) 
  [:#clj-candidate-list-container] (html/after (when-not (get-in data [:objective :drafting-started]) (post-invitation-container context)))
  [:#clj-candidate-list-container html/any-node] (html/replace-vars translations))

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

(html/defsnippet post-answer-container
  "templates/answers/post-answer-container.html" [[:#clj-post-answer-container]] [{:keys [translations user ring-request data] :as context}]
  [:#clj-answer-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-post-answer-container :.response-form] (if user (html/content (answer-create context)) identity)
  [:#clj-post-answer-container html/any-node] (html/replace-vars translations))

;QUESTIONS
(html/defsnippet a-question
  "templates/questions/a-question.html" [:li] [question]
  [:a] (html/set-attr :href (str "/objectives/" (:objective-id question) "/questions/" (:_id question)))
  [:a] (html/content (text->p-nodes (:question question)))
  [:.question-date] (html/content (utils/iso-time-string->pretty-time (:_created_at question)))) 
  
(html/defsnippet question-create
  "templates/questions/question-create.html" [:#clj-question-create] [{:keys [data translations] :as context}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" (get-in data [:objective :_id]) "/questions"))
  [:#clj-question-create html/any-node] (html/replace-vars translations))

(html/defsnippet post-question-container
  "templates/questions/post-question-container.html" [:#clj-post-question-container] [{:keys [user ring-request translations] :as context}]
  [:#clj-question-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" (:uri ring-request)))
  [:#clj-post-question-container :.response-form] (if user (html/content (question-create context)) identity)
  [:#clj-post-question-container html/any-node] (html/replace-vars translations))

;TODO - why is this breaking?
(html/defsnippet question-list-page
  "templates/questions/question-list.html" [:#clj-question-list] [{:keys [translations data users uri] :as context}]
  [:#objective-crumb] (html/set-attr :title (get-in data [:objective :title]))
  [:#objective-crumb] (html/content (get-in data [:objective :title]))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id])))
  [:#questions-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/questions"))
  [:h1] (html/after (article-meta (:objective data) translations))
  [:#clj-question-list :h1] (html/content (get-in data [:objective :title]))
  [:#clj-question-list :.question-list] (let [questions (:questions data)] (if (empty? questions) identity (html/content (map a-question questions)))) 
  [:#clj-question-list] (html/after (when-not (get-in data [:objective :drafting-started])
                                      (post-question-container context)))
  [:#clj-question-list html/any-node] (html/replace-vars translations))

(html/defsnippet question-view-page
  "templates/questions/question-view.html" [:#clj-question-view] [{:keys [translations data users ring-request] :as context}]
  [:#objective-crumb] (html/set-attr :title (get-in data [:objective :title]))
  [:#objective-crumb] (html/content (get-in data [:objective :title]))
  [:#objective-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id])))
  [:#questions-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/questions"))
  [:#question-crumb] (html/set-attr :href (str "/objectives/" (get-in data [:objective :_id]) "/questions/" (get-in data [:question :_id])))
  [:#question-crumb] (html/set-attr :title (get-in data [:question :question]))
  [:#question-crumb] (html/content (get-in data [:question :question]))
  [:#clj-question-view :h1] (html/content (get-in data [:question :question]))
  [:h1] (html/after (article-meta (:objective data) translations))
  [:#clj-question-view :.answer-list]  (if (empty? (:answers data)) identity (html/content (map an-answer (:answers data))))
  [:#clj-question-view] (html/after (when-not (get-in data [:objective :drafting-started]) (post-answer-container context)))
  [:#clj-question-view html/any-node] (html/replace-vars translations))

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
  "templates/objectives-list.html" [[:#clj-objectives-list]] [{:keys [translations data user]}]
  [:#clj-create-objective-link] (if user (html/html-content "<a href='objectives/create' class='button index-get-started' title='${objective-list/create-button-title}'>${objective-list/create-button-text}</a>") identity)
  [:ol] (html/content (map objective-list-entry (:objectives data)))
  [:#clj-objectives-list html/any-node] (html/replace-vars translations))

(html/defsnippet objective-create-page
  "templates/objectives-create.html" [[:#clj-objective-create]] [{:keys [translations]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-objective-create html/any-node] (html/replace-vars translations))

(html/defsnippet objective-detail-page
  "templates/objectives-detail.html" [[:#clj-objectives-detail]] [{:keys [translations data user ring-request] :as context}]
  [:#clj-objectives-detail html/any-node] (html/replace-vars translations)
  [:h1] (html/content (get-in data [:objective :title]))
  [:h1] (html/after (article-meta (:objective data) translations))
  [:#clj-obj-goals-value] (html/content (map a-goal (get-in data [:objective :goals])))
  [:#clj-obj-background-label] (if (empty? (get-in data [:objective :description])) nil identity)
  [:#clj-obj-background-label] (html/after (text->p-nodes (get-in data [:objective :description])))
  [:#clj-objectives-detail] (html/after (when-not (get-in data [:objective :drafting-started]) (comments-view context))))


;USERS
(html/defsnippet sign-up
  "templates/sign-up.html" [[:#clj-sign-up]] [{:keys [translations doc]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-username-error] (if-let [error-type (get-in doc [:errors :username])]
                           (html/content (translations (keyword "sign-up" (name error-type))))) 
  [:#clj-sign-up html/any-node] (html/replace-vars translations))


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
