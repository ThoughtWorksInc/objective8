(ns objective8.integration.translation
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [objective8.front-end.translation :as tr]
            [objective8.config :as config]))

(defn test-resource-locator [locale-keyword resource-file]
  (fn [] {:resource-name locale-keyword
          :resource (io/reader (io/file "test" "objective8" "integration" "fixtures" resource-file))}))

(fact "about loading a translation resource"
      (fact "loads translations from a csv resource"
            (tr/load-translation
             (test-resource-locator :language "language.csv"))
            => {:status ::tr/success
                :result {:language
                         {:template-1 {:tag-1 "template 1 tag 1 content"
                                       :tag-2 "template 1 tag 2 content"}
                          :template-2 {:tag-1 "template 2 tag 1 content"
                                       :tag-2 "template 2 tag 2 Objective[8]"}}}})

      (fact "attempting to parse bad translation resources reports an error"
            (tr/load-translation (test-resource-locator :rn "error--lookup-path-too-long.csv"))
            => {:status ::tr/parse-error
                :message "Translation lookup path too long"
                :resource-name :rn}
            (tr/load-translation (test-resource-locator :rn "error--lookup-path-too-short.csv"))
            => {:status ::tr/parse-error
                :message "Translation lookup path too short"
                :resource-name :rn})
      (fact "loads a translation with all substitution variables"
            (binding [config/environment (assoc config/environment
                                           :app-name "Policy Maker"
                                           :stonecutter-name "D-Cent SSO"
                                           :stonecutter-admin-email "admin@policy-maker.com")]
              (last (tr/replace-key ["index" "app-name" "The app is called %app-name%"]))
              => "The app is called Policy Maker"
              (last (tr/replace-key ["sign-in" "sign-in-with-d-cent" "Sign in with %stonecutter-name%"]))
              => "Sign in with D-Cent SSO"
              (last (tr/replace-key ["error-authorisation" "page-intro" "Please contact the site administrator at %stonecutter-admin-email%"]))
              => "Please contact the site administrator at admin@policy-maker.com")))

(facts "about loading a set of translation resources"
       (fact "generates a translation dictionary"
             (tr/load-translations [(test-resource-locator :l1 "l1.csv")
                                    (test-resource-locator :l2 "l2.csv")]) => {:l1 {:template-1 {:tag-1 "a"}}
                                                                               :l2 {:template-1 {:tag-1 "b"}}})

       (fact "aborts with an exception when there is an error loading a translation file"
             (tr/load-translations [(test-resource-locator :l1 "l1.csv")
                                    (test-resource-locator :e1 "error--lookup-path-too-long.csv")
                                    (test-resource-locator :e2 "error--lookup-path-too-short.csv")])
             => (throws Exception)))

(fact "live site translation files are correctly loaded"
      (tr/configure-translations) => map?)
