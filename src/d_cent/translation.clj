(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en {:index {:welcome "Hello"
                             :title "dCent"
                             :twitter-sign-in "Sign in with twitter"
                             :sign-in-required-message "Please sign in"}
                     :proposal {:title-label "Title"
                                :description-label "Description"
                                :objectives-label "Objectives"
                                :submit "Submit"
                                :proposal-link-text "Your new policy is here"}}
                :es {:index {:welcome "Hola"
                             :title "dCent"
                             :twitter-sign-in "Entrar con Twitter"
                             :sign-in-required-message "Entrar, por favor"}
                     :proposal {:title-label "Título"
                                :description-label "Descripción"
                                :objectives-label "Objetivos"
                                :submit "Entregar"
                                :proposal-link-text "Su nueva propuesta esta aqui"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
