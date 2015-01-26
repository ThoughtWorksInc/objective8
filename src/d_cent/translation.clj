(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en {:index {:doc-title "dCent"
                             :doc-description "dCent is description"
                             :twitter-sign-in "Sign in with twitter"
                             :sign-in-required-message "Please sign in"
                             :objective-create-btn-text "Create an objective"}
                     :sign-in {:doc-title "Sign in | dCent"
                               :doc-description "Sign in ..."
                               :twitter-sign-in "Sign in with twitter"}
                     :objective-create {:doc-title "Create an Objective | dCent"
                                        :doc-description "Create an Objective  ..."
                                        :page-title "Create an objective"
                                        :title-label "Title"
                                        :title-title "3 characters minimum, 120 characters maximum"
                                        :actions-label "Actions"
                                        :actions-title "50 characters minimum, 500 characters maximum"
                                        :description-label "Description"
                                        :description-title "Enter an optional description up to 1000 characters"
                                        :end-date "End date"
                                        :end-date-title "Please enter an end date"
                                        :submit "Submit"
                                        :objective-link-text "Your new objective is here"}
                     :objective-new-link {:objective-link-text "Your new objective is here"}
                     :objective-view {:doc-title "Objective | dCent"
                                      :doc-description "Objective  ..."
                                      :description-label "Description"
                                      :actions-label "Actions"
                                      :end-date-label "End date"}}
                :es {:index {:doc-title "Spanish(dCent)"
                             :doc-description "Spanish(dCent is description)"
                             :twitter-sign-in "Spanish(Sign in with twitter)"
                             :sign-in-required-message "Spanish(Please sign in)"
                             :objective-create-btn-text "Spanish(Create an objective)"}
                     :sign-in {:doc-title "Spanish(Sign in | dCent)"
                               :doc-description "Spanish(Sign in ...)"
                               :twitter-sign-in "Spanish(Sign in with twitter)"}
                     :objective-create {:doc-title "Spanish(Create an Objective | dCent)"
                                        :doc-description "Spanish(Create an Objective  ...)"
                                        :page-title "Spanish(Create an objective)"
                                        :title-label "Spanish(Title)"
                                        :title-title "Spanish(3 characters minimum, 120 characters maximum)"
                                        :actions-label "Spanish(Actions)"
                                        :actions-title "Spanish(50 characters minimum, 500 characters maximum)"
                                        :description-label "Spanish(Description)"
                                        :description-title "Spanish(Enter an optional description up to 1000 characters)"
                                        :end-date "Spanish(End date)"
                                        :end-date-title "Spanish(Please enter an end date)"
                                        :submit "Spanish(Submit)"
                                        :objective-link-text "Spanish(Your new objective is here)"}
                     :objective-new-link {:objective-link-text "Spanish(Your new objective is here)"}
                     :objective-view {:doc-title "Spanish(Objective | dCent)"
                                      :doc-description "Spanish(Objective  ...)"
                                      :description-label "Spanish(Description)"
                                      :actions-label "Spanish(Actions)"
                                      :end-date-label "Spanish(End date)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
