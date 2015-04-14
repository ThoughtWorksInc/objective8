(ns objective8.routes
  (:require [bidi.bidi :as bidi]
            [bidi.ring :refer [->Resources]]))

(def routes
  [
   "/"  ;; FRONT-END
   {""                  :fe/index
    "sign-in"           :fe/sign-in
    "sign-out"          :fe/sign-out
    "project-status"    :fe/project-status
    "learn-more"        :fe/learn-more
    "static/"           (->Resources {:prefix "public/"})
    "meta"              {"/up-vote"      {:post :fe/post-up-vote}
                         "/down-vote"    {:post :fe/post-down-vote}
                         "/comments"     {:post :fe/post-comment}
                         "/stars" {:post :fe/post-star}}
    "objectives"        {:get :fe/objective-list
                         :post :fe/create-objective-form-post
                         "/create" {:get :fe/create-objective-form} 
                         ["/" [#"\d+" :id]] {:get :fe/objective
                                             "/invite-writer" {:get :fe/invite-writer}
                                             "/writer-invitations" {:post :fe/invitation-form-post
                                                                    ["/" [#"\d+" :i-id]] {"/accept" {:post :fe/accept-invitation}
                                                                                          "/decline" {:post :fe/decline-invitation}
                                                                                          }}
                                             "/candidate-writers" {:get :fe/candidate-list}
                                             "/add-question" {:get :fe/add-a-question}
                                             "/questions" {:post :fe/add-question-form-post
                                                           :get :fe/question-list
                                                           ["/" [#"\d+" :q-id]] {:get :fe/question
                                                                                 "/answers" {:post :fe/add-answer-form-post}}}
                                             "/drafts" {:get :fe/draft-list
                                                        ["/" [#"\d+|latest" :d-id]] {:get :fe/draft}}
                                             "/add-draft" {:get :fe/add-draft-get
                                                           :post :fe/add-draft-post}
                                             "/import-draft" {:get :fe/import-draft-get
                                                              :post :fe/import-draft-post}}}
    "invitations"       {["/" :uuid] {:get :fe/writer-invitation}}

    ;; API
    "api/v1"            {"/users" {:post :api/post-user-profile
                                   :get :api/get-user-by-query
                                   ["/" [#"\d+" :id]] :api/get-user}

                         "/objectives" {:get :api/get-objectives
                                        :post :api/post-objective
                                        ["/" [#"\d+" :id]] {:get :api/get-objective
                                                            "/questions" {:post :api/post-question
                                                                          :get :api/get-questions-for-objective
                                                                          ["/" [#"\d+" :q-id]] {:get :api/get-question
                                                                                                "/answers" {:get :api/get-answers-for-question
                                                                                                            :post :api/post-answer}}}
                                                            "/candidate-writers" {:get :api/get-candidates-for-objective
                                                                                  :post :api/post-candidate-writer}
                                                            "/writer-invitations" {:post :api/post-invitation
                                                                                   ["/" [#"\d+" :i-id]] {:put :api/put-invitation-declination}}
                                                            "/drafts" {:post :api/post-draft
                                                                       :get :api/get-drafts-for-objective
                                                                       ["/" [#"\d+|latest" :d-id]] {:get :api/get-draft}}}}

                         "/meta" {"/comments" {:post :api/post-comment
                                               :get :api/get-comments}
                                  "/stars" {:post :api/post-star}
                                  "/marks" {:post :api/post-mark}}
                         "/up-down-votes" {:post :api/post-up-down-vote}
                         "/invitations" {:get :api/get-invitation}}}])
