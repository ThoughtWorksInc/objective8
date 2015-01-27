(ns d-cent.responses 
  (:require [net.cgrand.enlive-html :as html]
            [d-cent.translation :refer [translation-config]]))

(defn simple-response [text]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body text})

(defn render-template [template & args]
  (apply str (apply template args)))

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
                  ; [:meta (html/attr= :name "description")] (html/set-attr :content "Some text")
                  [:title] (html/content doc-title)
                  [:#clj-description] (html/set-attr :content doc-description)
                  [:#clj-global-navigation] (html/content global-navigation)
                  [:.header-logo] (html/content (translation :base/header-logo-text))
                  [:.header-logo] (html/set-attr :title (translation :base/header-logo-title))
                  [:#main-content] (html/content content))

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
                                                    [:a.button] (html/content (translation :index/objective-create-btn-text)))

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
                                                                          ;TODO look at attr= to remove classes here
                                                                          [:.clj-obj-title-label] (html/content (translation :objective-create/title-label))
                                                                          [:.clj-obj-title-title] (html/set-attr :title (translation :objective-create/title-title))
                                                                          [:.clj-obj-actions-label] (html/content (translation :objective-create/actions-label))
                                                                          [:.clj-obj-actions-title] (html/set-attr :title (translation :objective-create/actions-title))
                                                                          [:.clj-obj-description-label] (html/content (translation :objective-create/description-label))
                                                                          [:.clj-obj-description-title] (html/set-attr :title (translation :objective-create/description-title))
                                                                          [:.clj-obj-end-date-label] (html/content (translation :objective-create/end-date-label))
                                                                          [:.clj-obj-end-date-title] (html/set-attr :title (translation :objective-create/end-date-title))
                                                                          [:.button] (html/set-attr :value (translation :objective-create/submit)))

(html/defsnippet objectives-new-link-page "templates/objectives-new-link.html" [[:#clj-objectives-new-link]]
                                                                                [{:keys [translation stored-objective]}]
                                                                                [:h1] (html/content (translation :objective-new-link/page-title))
                                                                                [:#clj-objectives-new-link-text] (html/content (translation :objective-new-link/objective-link-text))
                                                                                [:a] (html/content stored-objective)
                                                                                [:a] (html/set-attr :href stored-objective))

(html/defsnippet objective-view-page "templates/objectives-view.html" [[:#clj-objectives-view]]
                                                                       [{:keys [translation objective]}]
                                                                       [:h1] (html/content (:title objective))
                                                                       [:#clj-obj-actions-label] (html/content (translation :objective-view/actions-label))
                                                                       [:#clj-obj-actions-value] (html/content (:actions objective))
                                                                       [:#clj-obj-description-label] (html/content (translation :objective-view/description-label))
                                                                       [:#clj-obj-description-value] (html/content (:description objective))
                                                                       [:#clj-obj-date-label] (html/content (translation :objective-view/end-date-label))
                                                                       [:#clj-obj-end-date-value] (html/content (:end-date objective)))


(defn rendered-response [template-name args]
  (let [navigation (if (:signed-in args)
                         global-navigation-signed-in
                         global-navigation-signed-out)]
    (simple-response (render-template base (assoc args :content (template-name args) :global-navigation (navigation args))))))
