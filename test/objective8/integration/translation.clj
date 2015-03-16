(ns objective8.integration.translation
  (:import [java.io File])
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [objective8.translation :as tr]))

(defn test-resource-locator [locale-keyword resource-file]
  (fn [] {:resource-name locale-keyword
          :resource (io/reader (io/file "test" "objective8" "integration" "fixtures" resource-file))}))

(fact "about loading a translation resource" :integration
      (fact "loads translations from a csv resource"
            (tr/load-translation
             (test-resource-locator :language "language.csv"))
            => {:status ::tr/success
                :result {:language
                         {:template-1 {:tag-1 "template 1 tag 1 content"
                                       :tag-2 "template 1 tag 2 content"}
                          :template-2 {:tag-1 "template 2 tag 1 content"
                                       :tag-2 "template 2 tag 2 content"}}}})

      (fact "attempting to parse bad translation resources reports an error"
            (tr/load-translation (test-resource-locator :rn "error--lookup-path-too-long.csv"))
            => {:status ::tr/parse-error
                :message "Translation lookup path too long"
                :resource-name :rn}
            (tr/load-translation (test-resource-locator :rn "error--lookup-path-too-short.csv"))
            => {:status ::tr/parse-error
                :message "Translation lookup path too short"
                :resource-name :rn}))

(facts "about loading a set of translation resources"
       (fact "generates a translation dictionary" :integration
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
