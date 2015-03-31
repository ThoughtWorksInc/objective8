(ns objective8.unit.page-furniture-test
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :refer :all] 
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

