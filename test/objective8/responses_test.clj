(ns objective8.responses-test
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [objective8.responses :as responses]
            [objective8.utils :as utils]))

(fact "'Sign-in to comment' link target on objective detail view refers back to current url"
      (-> (responses/objective-view-page {:translation (constantly "---")
                                          :objective {}
                                          :signed-in false
                                          :comments []
                                          :uri "CURRENT_PAGE_URI"})
          (html/select [:#clj-comment-sign-in :a])
          first)
      => (contains {:attrs (contains {:href (contains "refer=CURRENT_PAGE_URI")})}))
