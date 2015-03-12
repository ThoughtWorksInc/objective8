(ns objective8.translation-test
  (:import [java.io File])
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [objective8.translation :as tr]))

(defn test-resource-locator [language]
  (let [filename (str language ".csv")]
    (fn [] {:resource-name language
            :resource (io/reader (io/file "integration" filename))})))

(fact "loads translations from a csv resource" :integration
      (tr/load-translation (test-resource-locator "language")) => {:language {:template-1 {:tag-1 "template 1 tag 1 content"
                                                                                               :tag-2 "template 1 tag 2 content"}
                                                                                  :template-2 {:tag-1 "template 2 tag 1 content"
                                                                                               :tag-2 "template 2 tag 2 content"}}})

(fact "loads a set of translation resources and generates a translation dictionary" :integration
      (tr/load-translations [(test-resource-locator "l1")
                             (test-resource-locator "l2")]) => {:l1 {:template-1 {:tag-1 "a"}}
                                                                :l2 {:template-1 {:tag-1 "b"}}})
