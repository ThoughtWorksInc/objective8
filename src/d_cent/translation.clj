(ns d-cent.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en {:index {:welcome "Hello"
                             :title "dCent"
                             :twitter-login "Log in with Twitter"}
                     :proposal {:title-label "title"
                                :description-label "description"
                                :objectives-label "objectives"
                                :submit "submit"
                                :proposal-link-text "Your new proposal is here"}}
                :es {:index {:welcome "Hola"
                             :title "dCent"
                             :twitter-login "Entrar con Twitter"}
                     :proposal {:title-label "título"
                                :description-label "descripción"
                                :objectives-label "objetivos"
                                :submit "entregar"
                                :proposal-link-text "Su nueva propuesta esta aqui"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
