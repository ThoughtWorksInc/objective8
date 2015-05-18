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
    "admin-activity"    :fe/admin-activity
    "meta"              {"/up-vote"      {:post :fe/post-up-vote}
                         "/down-vote"    {:post :fe/post-down-vote}
                         "/comments"     {:post :fe/post-comment}
                         "/stars" {:post :fe/post-star}
                         "/marks" {:post :fe/post-mark}
                         "/writer-notes" {:post :fe/post-writer-note}
                         "/admin-removal-confirmation" {:post :fe/admin-removal-confirmation-post
                                                        :get :fe/admin-removal-confirmation-get}
                         "/admin-removals" {:post :fe/post-admin-removal}}
    "objectives"        {:get :fe/objective-list
                         :post :fe/create-objective-form-post
                         "/create" {:get :fe/create-objective-form} 
                         ["/" [#"\d+" :id]] {:get :fe/objective
                                             "/invite-writer" {:get :fe/invite-writer}
                                             "/writer-invitations" {:post :fe/invitation-form-post
                                                                    ["/" [#"\d+" :i-id]] {"/accept" {:post :fe/accept-invitation}
                                                                                          "/decline" {:post :fe/decline-invitation}
                                                                                          }}
                                             "/writers" {:get :fe/writers-list}
                                             "/add-question" {:get :fe/add-a-question}
                                             "/questions" {:post :fe/add-question-form-post
                                                           :get :fe/question-list
                                                           ["/" [#"\d+" :q-id]] {:get :fe/question
                                                                                 "/answers" {:post :fe/add-answer-form-post}}}
                                             "/drafts" {:get :fe/draft-list
                                                        ["/" [#"\d+|latest" :d-id]] {:get :fe/draft
                                                                                     "/diff" {:get :fe/draft-diff}
                                                                                     ["/sections/" [#"[0-9a-f]{8}" :section-label]] {:get :fe/draft-section
                                                                                                                                     "/annotations" {:post :fe/post-annotation}}}}
                                             "/add-draft" {:get :fe/add-draft-get
                                                           :post :fe/add-draft-post}
                                             "/import-draft" {:get :fe/import-draft-get
                                                              :post :fe/import-draft-post}
                                             "/dashboard" {"/questions" {:get :fe/dashboard-questions}
                                                           "/comments" {:get :fe/dashboard-comments}
                                                           "/annotations" {:get :fe/dashboard-annotations}}}}
    "invitations"       {["/" :uuid] {:get :fe/writer-invitation}}
    "create-profile"    {:get :fe/create-profile-get
                         :post :fe/create-profile-post}
    "edit-profile"      {:get :fe/edit-profile-get
                         :post :fe/edit-profile-post}
    "users"             {["/" :username] {:get :fe/profile}}
    "error"             {"/configuration" {:get :fe/error-configuration}}

    ;; API
    "api/v1"            {"/users" {:post :be/post-user-profile
                                   :get :be/get-user-by-query
                                   "/writer-profiles" {:put :be/put-writer-profile}
                                   ["/" [#"\d+" :id]] :be/get-user}

                         "/objectives" {:get :be/get-objectives
                                        :post :be/post-objective
                                        ["/" [#"\d+" :id]] {:get :be/get-objective
                                                            "/questions" {:post :be/post-question
                                                                          :get :be/get-questions-for-objective
                                                                          ["/" [#"\d+" :q-id]] {:get :be/get-question
                                                                                                "/answers" {:get :be/get-answers-for-question
                                                                                                            :post :be/post-answer}}}
                                                            "/writers" {:get :be/get-writers-for-objective
                                                                        :post :be/post-writer}
                                                            "/writer-invitations" {:post :be/post-invitation
                                                                                   ["/" [#"\d+" :i-id]] {:put :be/put-invitation-declination}}
                                                            "/drafts" {:post :be/post-draft
                                                                       :get :be/get-drafts-for-objective
                                                                       ["/" [#"\d+|latest" :d-id]] {:get :be/get-draft
                                                                                                    "/annotations" {:get :be/get-annotations}
                                                                                                    ["/sections/" [#"[0-9a-f]{8}" :section-label]] {:get :be/get-section}}}}}
                         "/writers" {["/" [#"\d+" :id]] {
                                     "/objectives" {:get :be/get-objectives-for-writer}}} 

                         "/meta" {"/comments" {:post :be/post-comment
                                               :get :be/get-comments}
                                  "/stars" {:post :be/post-star}
                                  "/marks" {:post :be/post-mark}
                                  "/writer-notes" {:post :be/post-writer-note}
                                  "/admin-removals" {:post :be/post-admin-removal
                                                     :get :be/get-admin-removals}}
                         "/up-down-votes" {:post :be/post-up-down-vote}
                         "/invitations" {:get :be/get-invitation}}}])
