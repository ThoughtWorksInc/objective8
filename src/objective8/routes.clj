(ns objective8.routes
  (:require [bidi.bidi :as bidi]
            [bidi.ring :refer [->Resources]]))

(def front-end-routes
  ["/"  ;; FRONT-END
   {""               :fe/index
    "sign-in"        :fe/sign-in
    "sign-out"       :fe/sign-out
    "project-status" :fe/project-status
    "learn-more"     :fe/learn-more
    "static/"        (->Resources {:prefix "public/"})
    "admin-activity" :fe/admin-activity
    "meta"           {"/up-vote"                    {:post :fe/post-up-vote}
                      "/down-vote"                  {:post :fe/post-down-vote}
                      "/comments"                   {:post :fe/post-comment}
                      "/stars"                      {:post :fe/post-star}
                      "/marks"                      {:post :fe/post-mark}
                      "/writer-notes"               {:post :fe/post-writer-note}
                      "/admin-removal-confirmation" {:post :fe/admin-removal-confirmation-post
                                                     :get  :fe/admin-removal-confirmation-get}
                      "/admin-removals"             {:post :fe/post-admin-removal}
                      "/promote-objective"              {:post :fe/post-promote-objective}}
    "objectives"     {:get               :fe/objective-list
                      :post              :fe/create-objective-form-post
                      "/create"          {:get :fe/create-objective-form}
                      ["/" [#"\d+" :id]] {:get                  :fe/objective
                                          "/comments"           {:get :fe/get-comments-for-objective}
                                          "/invite-writer"      {:get :fe/invite-writer}
                                          "/writer-invitations" {:post                :fe/invitation-form-post
                                                                 ["/" [#"\d+" :i-id]] {"/accept"  {:post :fe/accept-invitation}
                                                                                       "/decline" {:post :fe/decline-invitation}
                                                                                       }}
                                          "/writers"            {:get :fe/writers-list}
                                          "/add-question"       {:get :fe/add-a-question}
                                          "/questions"          {:post                :fe/add-question-form-post
                                                                 :get                 :fe/question-list
                                                                 ["/" [#"\d+" :q-id]] {:get       :fe/question
                                                                                       "/answers" {:post :fe/add-answer-form-post}}}
                                          "/drafts"             {:get                        :fe/draft-list
                                                                 ["/" [#"\d+|latest" :d-id]] {:get                                           :fe/draft
                                                                                              "/comments"                                    {:get :fe/get-comments-for-draft}
                                                                                              "/diff"                                        {:get :fe/draft-diff}
                                                                                              ["/sections/" [#"[0-9a-f]{8}" :section-label]] {:get           :fe/draft-section
                                                                                                                                              "/annotations" {:post :fe/post-annotation}}}}
                                          "/add-draft"          {:get  :fe/add-draft-get
                                                                 :post :fe/add-draft-post}
                                          "/import-draft"       {:get  :fe/import-draft-get
                                                                 :post :fe/import-draft-post}
                                          "/dashboard"          {"/questions"   {:get :fe/dashboard-questions}
                                                                 "/comments"    {:get :fe/dashboard-comments}
                                                                 "/annotations" {:get :fe/dashboard-annotations}}}}
    "invitations"    {["/" :uuid] {:get :fe/writer-invitation}}
    "create-profile" {:get  :fe/create-profile-get
                      :post :fe/create-profile-post}
    "edit-profile"   {:get  :fe/edit-profile-get
                      :post :fe/edit-profile-post}
    "users"          {["/" :username] {:get :fe/profile}}
    "error"          {"/configuration" {:get :fe/error-configuration}
                      "/log-in"        {:get :fe/error-log-in}}
    "authorisation"  :fe/authorisation-page
    "cookies"        :fe/cookies}])

(def back-end-routes
  ["/"
   {"api/v1" {"/users"         {:post              :api/post-user-profile
                                :get               :api/get-user-by-query
                                "/writer-profiles" {:put :api/put-writer-profile}
                                ["/" [#"\d+" :id]] :api/get-user}

              "/objectives"    {:get               :api/get-objectives
                                :post              :api/post-objective
                                ["/" [#"\d+" :id]] {:get                  :api/get-objective
                                                    "/questions"          {:post                :api/post-question
                                                                           :get                 :api/get-questions-for-objective
                                                                           ["/" [#"\d+" :q-id]] {:get       :api/get-question
                                                                                                 "/answers" {:get  :api/get-answers-for-question
                                                                                                             :post :api/post-answer}}}
                                                    "/writers"            {:get  :api/get-writers-for-objective
                                                                           :post :api/post-writer}
                                                    "/writer-invitations" {:post                :api/post-invitation
                                                                           ["/" [#"\d+" :i-id]] {:put :api/put-invitation-declination}}
                                                    "/drafts"             {:post                       :api/post-draft
                                                                           :get                        :api/get-drafts-for-objective
                                                                           ["/" [#"\d+|latest" :d-id]] {:get           :api/get-draft
                                                                                                        "/annotations" {:get :api/get-annotations}
                                                                                                        "/sections"    {:get                                  :api/get-sections
                                                                                                                        ["/" [#"[0-9a-f]{8}" :section-label]] {:get :api/get-section}}}}}}
              "/writers"       {["/" [#"\d+" :id]] {
                                                    "/objectives" {:get :api/get-objectives-for-writer}}}

              "/meta"          {"/comments"       {:post :api/post-comment
                                                   :get  :api/get-comments}
                                "/stars"          {:post :api/post-star}
                                "/marks"          {:post :api/post-mark}
                                "/writer-notes"   {:post :api/post-writer-note}
                                "/admin-removals" {:post :api/post-admin-removal
                                                   :get  :api/get-admin-removals}
                                "/promote-objective"  {:put :api/put-promote-objective}}
              "/up-down-votes" {:post :api/post-up-down-vote}
              "/invitations"   {:get :api/get-invitation}
              "/activities"    {:get :api/get-activities}}}])
