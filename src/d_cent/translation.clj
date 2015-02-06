(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en { :base {:header-logo-text "dCent Project"
                             :header-logo-title "Go to home page"
                             :browsehappy! "You are using an <strong>outdated</strong> browser. Please <a href=\"http://browsehappy.com/\">upgrade your browser</a> to improve your experience."}
                      :navigation-global {:sign-in-text "Sign in"
                                          :sign-in-title "Go to sign in"
                                          :sign-out-text "Sign out"
                                          :sign-out-title "Go to sign out"
                                          :profile-text "Profile"
                                          :profile-title "Go to user profile"}
                      :share-widget {:title "Share this page:"
                                     :facebook-title "Share on Facebook..."
                                     :facebook-text "Share on Facebook"
                                     :google-plus-title "Plus one this page on Google"
                                     :google-plus-text "Google +1"
                                     :twitter-title "Share this page on Twitter"
                                     :twitter-text "Tweet"
                                     :linkedin-title "Share this page on LinkedIn"
                                     :linkedin-text "Share on LinkedIn"
                                     :reddit-title "Submit this page to Reddit"
                                     :reddit-text "Submit to Reddit"
                                     :url-label "URL to share"
                                     :url-title "The link to this page"}
                      :index {:doc-title "dCent"
                             :doc-description "dCent is description"
                             :index-welcome "Collaborative policy making for democratic organisations."
                             :index-intro "Gather community opinion, generate ideas, share, discuss, vote and collaborate with experts to draft new policy."
                             :index-get-started "Get started"
                             :index-get-started-title "Get started"
                             :index-learn-more "Learn more"
                             :index-learn-more-title "Learn more"}
                     :sign-in {:doc-title "Sign in | dCent"
                               :doc-description "Sign in ..."
                               :page-title "Please sign in"
                               :twitter-sign-in-btn "Sign in with twitter"
                               :twitter-sign-in-title "Sign in with twitter"}
                     :objective-create {:doc-title "Create an Objective | dCent"
                                        :doc-description "Create an Objective  ..."
                                        :page-title "Create an objective"
                                        :title-label "Title"
                                        :title-title "3 characters minimum, 120 characters maximum"
                                        :goals-label "Goals"
                                        :goals-title "50 characters minimum, 500 characters maximum"
                                        :description-label "Description"
                                        :description-title "Enter an optional description up to 1000 characters"
                                        :end-date-label "End date"
                                        :end-date-title "Please enter an end date"
                                        :submit "Create"}
                     :objective-new-link {:doc-title "Success | dCent"
                                          :doc-description "Success! You created an objective ..."
                                          :page-title "Success"
                                          :objective-link-text "Your new objective is here"}
                     :objective-view {:doc-title "Objective | dCent"
                                      :doc-description "Objective  ..."
                                      :description-label "Description"
                                      :goals-label "Goals"
                                      :end-date-label "End date"
                                      :owner-label "Created by:"
                                      :created-message "Your objective has been created!"}
                     :comment-view {:created-message "Your comment has been added!"}
                     :users-email {:doc-title "Sign up | dCent"
                                   :doc-description "Email  ..."
                                   :page-title "Add your email"
                                   :user-email-welcome "Adding your email address will let us notify you when you receive responses to comments, objectives and votes."
                                   :email-label "Email"
                                   :email-title "Add your email title"
                                   :button "Submit" }}
                :es {:base {:header-logo-text "Spanish(dCent Project)"
                            :header-logo-title "Spanish(Go to home page)"
                            :browsehappy! "Spanish(You are using an <strong>outdated</strong> browser. Please <a href='http://browsehappy.com/' target='_blank' title='Visit browsehappy to learn more about the latest browser versions'>upgrade your browser</a> to improve your experience.)"}
                     :navigation-global {:sign-in-text "Spanish(Sign in)"
                                         :sign-in-title "Spanish(Go to sign in)"
                                         :sign-out-text "Spanish(Sign out)"
                                         :sign-out-title "Spanish(Go to sign out)"
                                         :profile-text "Spanish(Profile)"
                                         :profile-title "Spanish(Go to user profile)"}
                     :share-widget {:title "Spanish(Share this page:)"
                                     :facebook-title "Spanish(Share on Facebook...)"
                                     :facebook-text "Spanish(Share on Facebook)"
                                     :google-plus-title "Spanish(Plus one this page on Google)"
                                     :google-plus-text "Spanish(Google +1)"
                                     :twitter-title "Spanish(Share this page on Twitter)"
                                     :twitter-text "Spanish(Tweet)"
                                     :linkedin-title "Spanish(Share this page on LinkedIn)"
                                     :linkedin-text "Spanish(Share on LinkedIn)"
                                     :reddit-title "Spanish(Submit this page to Reddit)"
                                     :reddit-text "Spanish(Submit to Reddit)"
                                     :url-label "Spanish(URL to share)"
                                     :url-title "Spanish(The link to this page)"}
                     :index {:doc-title "Spanish(dCent)"
                             :doc-description "Spanish(dCent is description)"
                             :index-welcome "Spanish(Collaborative policy making for democratic organisations.)"
                             :index-intro "Spanish(Gather community opinion, generate ideas, share, discuss, vote and collaborate with experts to draft new policy.)"
                             :index-get-started "Spanish(Get started)"
                             :index-get-started-title "Spanish(Get started)"
                             :index-learn-more "Spanish(Learn more)"
                             :index-learn-more-title "Spanish(Learn more)"}
                     :sign-in {:doc-title "Spanish(Sign in | dCent)"
                               :doc-description "Spanish(Sign in ...)"
                               :page-title "Spanish(Please sign in)"
                               :twitter-sign-in-btn "Spanish(Sign in with twitter)"
                               :twitter-sign-in-title "Spanish(Sign in with twitter)"}
                     :objective-create {:doc-title "Spanish(Create an Objective | dCent)"
                                        :doc-description "Spanish(Create an Objective  ...)"
                                        :page-title "Spanish(Create an objective)"
                                        :title-label "Spanish(Title)"
                                        :title-title "Spanish(3 characters minimum, 120 characters maximum)"
                                        :goals-label "Spanish(Goals)"
                                        :goals-title "Spanish(50 characters minimum, 500 characters maximum)"
                                        :description-label "Spanish(Description)"
                                        :description-title "Spanish(Enter an optional description up to 1000 characters)"
                                        :end-date-label "Spanish(End date)"
                                        :end-date-title "Spanish(Please enter an end date)"
                                        :submit "Spanish(Create)"}
                     :objective-new-link {:doc-title "Spanish(Success | dCent)"
                                          :doc-description "Spanish(Success! You created an objective ...)"
                                          :page-title "Spanish(Success)"
                                          :objective-link-text "Spanish(Your new objective is here)"}
                     :objective-view {:doc-title "Spanish(Objective | dCent)"
                                      :doc-description "Spanish(Objective  ...)"
                                      :description-label "Spanish(Description)"
                                      :goals-label "Spanish(Goals)"
                                      :end-date-label "Spanish(End date)"
                                      :owner-label "Spanish(Created by:)"
                                      :created-message "Spanish(Your objective has been created!)"}
                    :comment-view {:created-message "Spanish(Your comment has been added!)"}
                     :users-email {:doc-title "Spanish(User email | dCent)"
                                   :doc-description "Spanish(Email  ...)"
                                   :page-title "Spanish(Add your email)"
                                   :user-email-welcome "Spanish(Adding your email address will let us notify you when you receive responses to comments, objectives and votes.)"
                                   :email-label "Spanish(Email)"
                                   :email-title "Spanish(Add your email title)"
                                   :button "Spanish(Submit)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
