(ns objective8.responses-test
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [objective8.responses :as responses]
            [objective8.utils :as utils]))

(fact "'Sign-in to comment' link target on objective detail view refers back to current url"
      (-> (responses/objective-detail-page {:translation (constantly "---")
                                            :objective {}
                                            :signed-in false
                                            :comments []
                                            :uri "CURRENT_PAGE_URI"})
          (html/select [:#clj-comment-sign-in-uri])
          first)
      => (contains {:attrs (contains {:href (contains "refer=CURRENT_PAGE_URI")})}))

(facts "Splitting text into paragraphs"
       (fact "Newlines are turned into paragraphs"
             (responses/text->p-nodes "Hello\nWorld") =>
             [[{:tag :p :content '("Hello") :attrs {}}]
              [{:tag :p :content '("World") :attrs {}}]])

       (fact "Collapses multiple newlines into single paragraphs"
             (responses/text->p-nodes "Hello\n\n\nWorld\n\r\nFoo\n   \n \n \nBar Zap\r\t\n") =>
             [[{:tag :p :content '("Hello") :attrs {}}]
              [{:tag :p :content '("World") :attrs {}}]
              [{:tag :p :content '("Foo") :attrs {}}]
              [{:tag :p :content '("Bar Zap") :attrs {}}]])

       (fact "Deals with nil text"
             (responses/text->p-nodes nil) => nil))

