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
                                        :description-label "Description"
                                        :actions-label "Actions"
                                        :submit "Submit"
                                        :objective-link-text "Your new objective is here"}
                     :objective-new-link {:objective-link-text "Your new objective is here"}
                     :objective-view {:doc-title "Objective | dCent"
                                      :doc-description "Objective  ..."
                                      :description-label "Description"
                                      :actions-label "Actions"}}
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
                                        :description-label "Spanish(Description)"
                                        :actions-label "Spanish(Actions)"
                                        :submit "Spanish(Submit)"
                                        :objective-link-text "Spanish(Your new objective is here)"}
                     :objective-new-link {:objective-link-text "Spanish(Your new objective is here)"}
                     :objective-view {:doc-title "Spanish(Objective | dCent)"
                                      :doc-description "Spanish(Objective  ...)"
                                      :description-label "Spanish(Description)"
                                      :actions-label "Spanish(Actions)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
