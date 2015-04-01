(ns objective8.unit.template-functions-test
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [objective8.templates.template-functions :refer :all] 
            [objective8.utils :as utils]))

(facts "Splitting text into paragraphs"
       (fact "Newlines are turned into paragraphs"
             (text->p-nodes "Hello\nWorld") =>
             [[{:tag :p :content '("Hello") :attrs {}}]
              [{:tag :p :content '("World") :attrs {}}]])

       (fact "Collapses multiple newlines into single paragraphs"
             (text->p-nodes "Hello\n\n\nWorld\n\r\nFoo\n   \n \n \nBar Zap\r\t\n") =>
             [[{:tag :p :content '("Hello") :attrs {}}]
              [{:tag :p :content '("World") :attrs {}}]
              [{:tag :p :content '("Foo") :attrs {}}]
              [{:tag :p :content '("Bar Zap") :attrs {}}]])

       (fact "Deals with nil text"
             (text->p-nodes nil) => nil))

(facts "About translating templates"
       (def translations :mock-translations-function)

       (fact "content is translated"
             (let [input-html-resource (html/html-resource (java.io.StringReader. "<p data-l8n=\"some-key/some-value\">!UNTRANSLATED_CONTENT</p>"))]
               (-> (translate {:translations translations} input-html-resource)
                   (html/select [:p])
                   first
                   (html/text)) => "TRANSLATED_CONTENT"
             (provided
               (translations :some-key/some-value) => "TRANSLATED_CONTENT"))))
