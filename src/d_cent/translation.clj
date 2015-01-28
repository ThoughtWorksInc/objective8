(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en { :base {:header-logo-text "dCent Project"
                             :header-logo-title "Go to home page"}
                      :navigation-global {:sign-in-text "Sign in"
                                          :sign-in-title "Go to sign in"
                                          :sign-out-text "Sign out"
                                          :sign-out-title "Go to sign out"
                                          :profile-text "Profile"
                                          :profile-title "Go to user profile"}
                      :index {:doc-title "dCent"
                             :doc-description "dCent is description"
                             :twitter-sign-in "Sign in with twitter"
                             :sign-in-required-message "Please sign in"
                             :objective-create-btn-text "Create an objective"}
                     :sign-in {:doc-title "Sign in | dCent"
                               :doc-description "Sign in ..."
                               :page-title "Sign in"
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
                                        :submit "Submit"}
                     :objective-new-link {:doc-title "Success | dCent"
                                          :doc-description "Success! You created an objective ..."
                                          :page-title "Success"
                                          :objective-link-text "Your new objective is here"}
                     :objective-view {:doc-title "Objective | dCent"
                                      :doc-description "Objective  ..."
                                      :description-label "Description"
                                      :goals-label "Goals"
                                      :end-date-label "End date"}
                     :users-email {:doc-title "User email | dCent"
                                   :doc-description "Email  ..."
                                   :page-title "Add your email"
                                   :email-label "Email"
                                   :email-title "Add your email title"
                                   :button "Submit"}}
                :es {:base {:header-logo-text "Spanish(dCent Project)"
                            :header-logo-title "Spanish(Go to home page)"}
                     :navigation-global {:sign-in-text "Spanish(Sign in)"
                                         :sign-in-title "Spanish(Go to sign in)"
                                         :sign-out-text "Spanish(Sign out)"
                                         :sign-out-title "Spanish(Go to sign out)"
                                         :profile-text "Spanish(Profile)"
                                         :profile-title "Spanish(Go to user profile)"}
                     :index {:doc-title "Spanish(dCent)"
                             :doc-description "Spanish(dCent is description)"
                             :twitter-sign-in "Spanish(Sign in with twitter)"
                             :sign-in-required-message "Spanish(Please sign in)"
                             :objective-create-btn-text "Spanish(Create an objective)"}
                     :sign-in {:doc-title "Spanish(Sign in | dCent)"
                               :doc-description "Spanish(Sign in ...)"
                               :page-title "Spanish(Sign in)"
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
                                        :submit "Spanish(Submit)"}
                     :objective-new-link {:doc-title "Spanish(Success | dCent)"
                                          :doc-description "Spanish(Success! You created an objective ...)"
                                          :page-title "Spanish(Success)"
                                          :objective-link-text "Spanish(Your new objective is here)"}
                     :objective-view {:doc-title "Spanish(Objective | dCent)"
                                      :doc-description "Spanish(Objective  ...)"
                                      :description-label "Spanish(Description)"
                                      :goals-label "Spanish(Goals)"
                                      :end-date-label "Spanish(End date)"}
                     :users-email {:doc-title "Spanish(User email | dCent)"
                                   :doc-description "Spanish(Email  ...)"
                                   :page-title "Spanish(Add your email)"
                                   :email-label "Spanish(Email)"
                                   :email-title "Spanish(Add your email title)"
                                   :button "Spanish(Submit)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
