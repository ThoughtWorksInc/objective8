(ns objective8.unit.auth-views
  (:require [midje.sweet :refer :all]
            [objective8.front-end.templates.page-furniture :as pf]
            [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.question :as question]
            [objective8.front-end.templates.objective :as objective]))

(def user "user123")
(def context-signed-in {:user         {:username user
                                       :email    "user@test.com"}
                        :translations (constantly (html/html-snippet))})
(def context-signed-out {:user         {:email "user@test.com"}
                         :translations (constantly (html/html-snippet))})

(def answer-data {:data {:answers   {:answer-1 {:_id 0}}
                         :question  {:_id  1
                                     :meta {:answers-count 1}}
                         :objective {:_id 2}
                         :offset    0}})
(def invitation-data {:data {:objective {:_id 3}}
                      :invitation-rsvp {:objective-id 3
                                        :invitation-id 4}})

(fact "masthead contains sign in link when not signed in"
      (-> (pf/masthead context-signed-out)
          (html/select [:.clj-masthead-sign-in])
          empty?) => false)

(fact "masthead contains username when signed in"
      (-> (pf/masthead context-signed-in)
          (html/select [:.clj-masthead-signed-in :.clj-username])
          first
          html/text) => user)

(fact "sign in to comment link is shown when not signed in"
      (-> (pf/comment-create context-signed-out nil)
          (html/select [:.clj-to-comment-sign-in-link])
          first
          :attrs
          :href) => (contains "/sign-in"))

(fact "comment form is shown when signed in"
      (-> (pf/comment-create context-signed-in :objective)
          (html/select [:.clj-add-comment-form])
          empty?) => false)

(fact "upvote/downvote forms on objective page go to sign in endpoint when not signed in"
      (-> (pf/comment-item context-signed-out {:_created_at "2015-07-14T09:00:00.000Z"})
          (html/select [:.clj-up-down-vote-form])
          first
          :attrs
          :action) => "/sign-in")

(fact "upvote/downvote forms on objective page go to meta api endpoints when signed in"
      (-> (pf/comment-item context-signed-in {:_created_at "2015-07-14T09:00:00.000Z"})
          (html/select [:.clj-up-down-vote-form])
          first
          :attrs
          :action) => "/meta/up-vote")

(fact "sign in to answer question link is shown when not signed in"
      (-> (question/question-page (merge context-signed-out answer-data))
          (html/select [:.clj-to-add-answer-sign-in-link])
          first
          :attrs
          :href) => (contains "/sign-in"))

(fact "answer question form is shown when signed in"
      (-> (question/question-page (merge context-signed-in answer-data))
          (html/select [:.clj-answer-form])
          empty?) => false)

(fact "upvote/downvote forms on answer question page directs to sign in page when not signed in"
      (-> (question/question-page (merge context-signed-out answer-data))
          (html/select [:.clj-answer :.clj-approval-form])
          first
          :attrs
          :action) => "/sign-in")

(fact "upvote/downvote forms on answer question page go to meta api endpoints when signed in"
      (-> (question/question-page (merge context-signed-in answer-data))
          (html/select [:.clj-answer :.clj-approval-form])
          first
          :attrs
          :action) => "/meta/up-vote")

(fact "accept an invitation link is shown when not signed in"
      (-> (objective/objective-page (merge context-signed-out invitation-data))
          (html/select [:.clj-modal-contents :.clj-anchor-button])
          first
          :attrs
          :href) => (contains "/sign-in"))

(fact "accept an invitation form is shown when signed in"
      (-> (objective/objective-page (merge context-signed-in invitation-data))
          (html/select [:.clj-invitation-response-accept])
          empty?) => false)

(fact "star form goes to sign in endpoint when not signed in"
      (-> (objective/objective-page (merge context-signed-out {:data {:objective {:_id 3}}}))
          (html/select [:.clj-star-form])
          first
          :attrs
          :action) => "/sign-in")

(fact "star form goes to meta api endpoint when signed in"
      (-> (objective/objective-page (merge context-signed-in {:data {:objective {:_id 3}}}))
          (html/select [:.clj-star-form])
          first
          :attrs
          :action) => "/meta/stars")