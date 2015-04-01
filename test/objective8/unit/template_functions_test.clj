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
             (let [input-html-resource (html/html-resource (java.io.StringReader. "<p data-l8n=\"content:some-key/some-value\">!UNTRANSLATED_CONTENT</p>"))]
               (-> (translate {:translations translations} input-html-resource)
                   (html/select [:p])
                   first
                   (html/text)) => "TRANSLATED_CONTENT"
               (provided
                 (translations :some-key/some-value) => "TRANSLATED_CONTENT")))

       (fact "static html content can be used as a translation"
             (let [input-html-resource (html/html-resource (java.io.StringReader. "<p data-l8n=\"html:some-key/some-value\">!UNTRANSLATED_NON_HTML_CONTENT</p>"))]
               (-> (translate {:translations translations} input-html-resource)
                   (html/select [:p])
                   first
                   (html/select [:.static-html-class])
                   first
                   (html/text)) => "TRANSLATED_CONTENT_INSIDE_DIV"
               (provided
                 (translations :some-key/some-value) => "<div class=\"static-html-class\">TRANSLATED_CONTENT_INSIDE_DIV</div>"))) 

       (fact "html attributes can be translated"
             (let [input-html-resource (html/html-resource (java.io.StringReader. "<p data-l8n=\"attr/title:some-key/some-value\" title=\"UNTRANSLATED_TITLE_ATTRIBUTE\">!SOME_CONTENT</p>"))]
               (-> (translate {:translations translations} input-html-resource)
                   (html/select [:p])
                   first
                   :attrs
                   :title) => "TRANSLATED_TITLE_ATTRIBUTE"
               (provided
                 (translations :some-key/some-value) => "TRANSLATED_TITLE_ATTRIBUTE")))

       (fact "can translate content and attributes in same element"
             (against-background
               (translations :attr-key/attr-value) => "TRANSLATED_TITLE_ATTRIBUTE"
               (translations :content-key/content-value) => "TRANSLATED_CONTENT") 
             (let [data-l8n-string "attr/title:attr-key/attr-value content:content-key/content-value"
                   input-html-string (str "<p data-l8n=\"" data-l8n-string 
                                          "\" title=\"UNTRANSLATED_TITLE_ATTRIBUTE\">!UNTRANSLATED_CONTENT</p>")
                   input-html-resource (html/html-resource (java.io.StringReader. input-html-string))
                   translated-resource (translate {:translations translations} input-html-resource)]
               (-> translated-resource
                   (html/select [:p])
                   first
                   :attrs
                   :title) => "TRANSLATED_TITLE_ATTRIBUTE"
               (-> translated-resource
                   (html/select [:p])
                   first
                   (html/text)) => "TRANSLATED_CONTENT")))
