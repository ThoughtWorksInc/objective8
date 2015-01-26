(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en {:index {:welcome "Hello"
                             :title "dCent"
                             :twitter-sign-in "Sign in with twitter"
                             :sign-in-required-message "Please sign in"}
                     :objective {:title-label "Title"
                                :description-label "Description"
                                :actions-label "Actions"
                                :submit "Submit"
                                :objective-link-text "Your new objective is here"}}
                :es {:index {:welcome "Hola"
                             :title "dCent"
                             :twitter-sign-in "Entrar con Twitter"
                             :sign-in-required-message "Entrar, por favor"}
                     :objective {:title-label "Título"
                                :description-label "Descripción"
                                :actions-label "Spanish(Actions)"
                                :submit "Entregar"
                                :objective-link-text "Spanish(Your new objective is here)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
