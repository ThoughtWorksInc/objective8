(ns objective8.routes
  (:require [bidi.bidi :as bidi]
            [bidi.ring :refer [->Resources]]))

(def routes
  [
   "/"  ;; FRONT-END
        {""                 :index
        "sign-in"           :sign-in
        "sign-out"          :sign-out
        "project-status"    :project-status
        "learn-more"        :learn-more
        "static/"           (->Resources {:prefix "public/"})
        "objectives"        {:get :objective-list
                             :post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] {:get :objective
                                        "/writer-invitations" {:post :invitation-form-post
                                                               ["/" :i-id] {:get :accept-or-decline-invitation
                                                                            "/accept" {:post :accept-invitation}
                                                                            "/decline" {:post :decline-invitation}
                                                                            }}
                                        "/candidate-writers" {:get :candidate-list}
                                        "/questions" {:post :add-question-form-post
                                                      :get :question-list
                                                      ["/" :q-id] {:get :question
                                                                   "/answers" {:post :add-answer-form-post}}}
                                        "/drafts" {:get :current-draft
                                                   ["/" :d-id] {:get :fe/draft}}
                                        "/edit-draft" {:get :fe/edit-draft-get
                                                       :post :fe/edit-draft-post}}}
        "comments"          {:post :create-comment-form-post}
        "invitations"       {["/" :uuid] {:get :writer-invitation}}

        ;; API
        "api/v1"            {"/users" {:post :post-user-profile
                                       :get :get-user-by-query
                                       ["/" :id] :get-user}

                             "/objectives" {:get :get-objectives
                                            :post :post-objective
                                            ["/" :id] {:get :get-objective
                                                       "/comments" :get-comments-for-objective
                                                       "/questions" {:post :post-question
                                                                     :get :get-questions-for-objective
                                                                     ["/" :q-id] {:get :get-question
                                                                                  "/answers" {:get :get-answers-for-question
                                                                                              :post :post-answer}}}
                                                       "/candidate-writers" {:get :get-candidates-for-objective
                                                                             :post :post-candidate-writer}
                                                       "/writer-invitations" {:post :post-invitation
                                                                              ["/" :i-id] {:put :put-invitation-declination}}
                                                       "/drafts" {:post :api/post-draft
                                                                  ["/" :d-id] {:get :api/get-draft}}}}

                             "/comments"   {:post :post-comment}
                             "/invitations" {:get :get-invitation}}

         ;;DEV-API
         "dev/api/v1"     {["/objectives/" :id "/start-drafting"] {:post :post-start-drafting}}}])
