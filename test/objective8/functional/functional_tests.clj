(ns objective8.functional.functional-tests 
  (:require [midje.sweet :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clj-webdriver.taxi :as wd]
            [clj-webdriver.core :as wc]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [objective8.core :as core]
            [objective8.utils :as utils]
            [objective8.actions :as actions]
            [objective8.integration.integration-helpers :as integration-helpers]
            [dev-helpers.stub-twitter :refer [stub-twitter-auth-config]]))

(def config-without-twitter (assoc core/app-config :authentication stub-twitter-auth-config))

(defn wait-for-title [title]
  (try 
    (wd/wait-until #(= (wd/title) title) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>>> Title never appeared:"))
      (prn (str "Expected: " title))
      (prn (str "Actual: " (wd/title)))
      (throw e))))

(def not-empty? (comp not empty?))

(defn wait-for-element [q]
  (try
    (wd/wait-until #(not-empty? (wd/elements q)) 5000)
    (catch Exception e
      (prn (str "Could not find element: " q))
      (throw e))))

(defn wait-for [pred]
  (try
    (wd/wait-until pred 5000)
    (catch Exception e
      (prn (str "Waiting for predicate failed"))
      (throw e))))

(def screenshot-directory "test/objective8/functional/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [filename]
  (wd/take-screenshot :file (str screenshot-directory "/"
                                 (format "%02d" (swap! screenshot-number + 1))
                                 "_" filename ".png")))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))

(def journey-state (atom nil))

(def FIRST_DRAFT_MARKDOWN  "A heading\n===\nSome content")
(def SECOND_DRAFT_MARKDOWN  "A heading\n===\nSome content\nSome more content")
(def THIRD_DRAFT_MARKDOWN  "A heading\n===\nSome content\nSome more content\nAnother line of content")
(def FIRST_DRAFT_HTML (utils/hiccup->html (utils/markdown->hiccup FIRST_DRAFT_MARKDOWN)))
(def THIRD_DRAFT_HTML (utils/hiccup->html (utils/markdown->hiccup THIRD_DRAFT_MARKDOWN)))

(facts "About user journeys"
       (against-background 
         [(before :contents (do (integration-helpers/db-connection)
                                (integration-helpers/truncate-tables)
                                (core/start-server config-without-twitter)
                                (wd/set-driver! {:browser :firefox})
                                (reset! journey-state {})
                                (clear-screenshots)))
          (after :contents (do (wd/quit)
                               (integration-helpers/truncate-tables)
                               (core/stop-server)))]
         (fact "can add an objective"
               (try (wd/to "localhost:8080")
                    (wait-for-title "Objective[8]")
                    (screenshot "home_page")

                    (wd/click "a[href='/objectives']") 
                    (wait-for-title "Objectives | Objective[8]")
                    (screenshot "objectives_page")

                    (wd/click "a[href='/objectives/create']") 
                    (wait-for-title "Sign in or Sign up | Objective[8]")
                    (screenshot "sign_in_page")

                    (wd/click ".func--sign-in-with-twitter") 
                    (wait-for-title "Sign up | Objective[8]")
                    (screenshot "sign_up_almost_there")

                    (wd/input-text "#username" "funcTestUser123")
                    (-> "#email-address" 
                        (wd/input-text "func_test_user@domain.com")
                        wd/submit) 

                    (screenshot "create_objective_page")
                    (wait-for-title "Create an Objective | Objective[8]")

                    (wd/input-text ".func--input-objective-title" "Functional test headline")
                    (wd/input-text ".func--input-objective-goal-1" "Functional test goal")
                    (-> ".func--input-objective-background"
                        (wd/input-text 
                          "Functional test description with lots of hipster-ipsum:
                          Master cleanse squid nulla, ugh kitsch biodiesel cronut food truck. Nostrud Schlitz tempor farm-to-table skateboard, wayfarers adipisicing Pitchfork sunt Neutra brunch four dollar toast forage placeat. Fugiat lo-fi sed polaroid Portland et tofu Austin. Blue Bottle labore forage, in bitters incididunt ugh delectus seitan flannel. Mixtape migas cardigan, quis American Apparel culpa aliquip cupidatat et nisi scenester. Labore sriracha Etsy flannel XOXO. Normcore selvage do vero keytar synth.")
                        wd/submit) 

                    (wait-for-title "Functional test headline | Objective[8]")
                    (swap! journey-state assoc :objective-url (wd/current-url))
                    (screenshot "objective_page")

                    (wd/title)

                    (catch Exception e
                      (screenshot "ERROR-Can-add-an-objective") 
                      (throw e)))
               =>  "Functional test headline | Objective[8]") 

         (fact "Can add a question"
               (try (wd/to "localhost:8080/objectives")
                    (wait-for-title "Objectives | Objective[8]")
                    (screenshot "objectives_page_with_an_objective")

                    (wd/click ".func--objective-list-item-link")
                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "objective_page")

                    (wd/click ".func--add-question")
                    (wait-for-element ".func--question-textarea")
                    (screenshot "add_question_page")

                    (-> ".func--question-textarea"
                        (wd/input-text "Functional test question") 
                        (wd/submit)) 

                    (wait-for-element ".func--add-question")
                    (screenshot "objective_page_with_question")

                    (catch Exception e
                      (screenshot "Error-Can-add-questions")
                      (throw e))))

         (fact "Can answer a question"
               (try (wd/to (:objective-url @journey-state))
                    (wait-for-element ".func--answer-link")

                    (wd/click ".func--answer-link")
                    (wait-for-element ".func--add-answer")

                    (-> ".func--add-answer"
                        (wd/input-text "Functional test answer") 
                        (wd/submit))

                    (wait-for-element ".func--answer-text")
                    (screenshot "answered_question")
                    (swap! journey-state assoc :question-url (wd/current-url))

                    (wd/text ".func--answer-text")

                    (catch Exception e
                      (screenshot "Error-Can-answer-questions")
                      (throw e)))
               => "Functional test answer")

         (fact "Can up vote an answer" 
               (try (wd/to (:question-url @journey-state))
                    
                    (wait-for-element "textarea.func--add-answer")
                    (wd/text ".func--up-score") => "0"

                    (wd/click "button.func--up-vote")

                    (wait-for-element "textarea.func--add-answer")
                    (wd/text ".func--up-score") => "1"
                    
                    (catch Exception e
                      (screenshot "Error-Can-vote-on-an-answer")
                      (throw e))))

         (fact "Can invite a writer"
           (try (wd/to (:objective-url @journey-state))
                (wait-for-title "Functional test headline | Objective[8]")
                (screenshot "objective_page")

                (wd/click ".func--invite-writer")
                (wait-for-element ".func--invite-writer")
                (screenshot "invite_writer_page")

                (wd/input-text ".func--writer-name" "Functional Test Writer Name")
                (wd/input-text ".func--writer-email" "func_test_candidate@domain.com")
                (-> ".func--writer-reason"
                  (wd/input-text "Functional test invitation reason")
                  wd/submit)
                (wait-for-title "Functional test headline | Objective[8]")
                (screenshot "objective_with_invitation_flash")

                (->> (wd/value ".func--invitation-url")
                  (re-find #"http://.*$")
                  (swap! journey-state assoc :invitation-url))
                {:page-title (wd/title)
                 :flash-message (wd/text ".func--invitation-guidance")}

                (catch Exception e
                  (screenshot "ERROR-Can-invite-a-writer")
                  (throw e)))
           => (contains {:page-title "Functional test headline | Objective[8]"
                         :flash-message (contains "Your writer's invitation")}))


         (fact "Can accept a writer invitation"
               (try (wd/to (:invitation-url @journey-state))
                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "objective_page_after_hitting_invitation_url")

                    (wd/click ".func--invitation-accept")
                    (wait-for-element ".func--objective-page")
                    (screenshot "objective_page_from_recently_added_writer")

                    (wd/text ".func--writer-name")

                    (catch Exception e
                      (screenshot "ERROR-Can-accept-a-writer-invitation")
                      (throw e)))
               => "Functional Test Writer Name")

          (against-background
            [(before :contents (-> (:objective-url @journey-state)
                                   (string/split #"/")
                                   last
                                   Integer/parseInt
                                   actions/start-drafting!))]
            (fact "Can submit a draft"
                  (try
                    (wd/to (str (:objective-url @journey-state) "/drafts/latest"))
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "latest_draft_no_draft")

                    (wd/click ".func--add-a-draft")
                    (wait-for-title "Add draft | Objective[8]")
                    (screenshot "add_draft_empty")

                    (wd/input-text ".func--add-draft-content" FIRST_DRAFT_MARKDOWN)
                    (wd/click ".func--preview-action")
                    (wait-for-title "Add draft | Objective[8]")
                    (screenshot "preview_draft")

                    (wd/click ".func--submit-action")

                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "submitted_draft")

                    {:page-title (wd/title)
                     :page-source (wd/page-source)}

                    (catch Exception e
                      (screenshot "ERROR-Can-submit-a-draft")
                      (throw e)))
                  => (contains {:page-title "Policy draft | Objective[8]"
                                :page-source (contains FIRST_DRAFT_HTML)}))

            (fact "Can view latest draft"
                  (try
                    (wd/to (:objective-url @journey-state))
                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "drafting_started_objective")

                    (wd/click ".func--drafting-message-link")
                    (wait-for-title "Drafts | Objective[8]")
                    (screenshot "drafts_list_with_one_draft")

                    (wd/click ".func--latest-draft-link")
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "latest_draft")

                    {:page-title (wd/title)
                     :page-source (wd/page-source)}

                    (catch Exception e
                      (screenshot "ERROR-Can-view-latest-draft")
                      (throw e)))
                  => (contains {:page-title "Policy draft | Objective[8]"
                                :page-source (contains FIRST_DRAFT_HTML)}))

            (fact "Can comment on a draft"
                  (try
                    (wd/to (str (:objective-url @journey-state) "/drafts/latest"))
                    (wait-for-title "Policy draft | Objective[8]")

                    (wd/input-text ".func--comment-form-text-area" "Functional test comment text")
                    (wd/click ".func--comment-form-submit")
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "draft_with_comment")

                    (wd/page-source)

                    (catch Exception e
                      (screenshot "ERROR-Can-comment-on-a-draft")
                      (throw e)))
                  => (contains "Functional test comment text"))

            (fact "Can down vote a comment on a draft"
                  (try
                    (wd/to (str (:objective-url @journey-state) "/drafts/latest"))
                    (wait-for-title "Policy draft | Objective[8]")

                    (wd/text ".func--down-score") => "0"
                    
                    (wd/click "button.func--down-vote")

                    (wait-for-title "Policy draft | Objective[8]")
                    (wd/text ".func--down-score") => "1"
                    
                    (catch Exception e
                      (screenshot "ERROR-Can-vote-on-a-comment-on-a-draft")
                      (throw e))))

            (fact "Can navigate between drafts"
                  (try
                    (wd/to (str (:objective-url @journey-state) "/drafts"))
                    (wait-for-title "Drafts | Objective[8]")
                    (screenshot "list_of_drafts")

                    (wd/click ".func--add-a-draft")
                    (wait-for-title "Add draft | Objective[8]")
                    (wd/input-text ".func--add-draft-content" SECOND_DRAFT_MARKDOWN)

                    (wd/click ".func--submit-action")
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "second_draft")

                    (wd/click ".func--add-a-draft")
                    (wait-for-title "Add draft | Objective[8]")
                    (wd/input-text ".func--add-draft-content" THIRD_DRAFT_MARKDOWN)

                    (wd/click ".func--submit-action")
                    (wait-for-title "Policy draft | Objective[8]")

                    (wd/to (str (:objective-url @journey-state) "/drafts/latest"))
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "latest_draft_with_previous_button")

                    (wd/click ".func--draft-version-previous-link")
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "second_draft_with_next")

                    (wd/click ".func--draft-version-next-link")
                    (wait-for-title "Policy draft | Objective[8]")
                    (screenshot "third_draft")

                    (wd/page-source)
                    (catch Exception e
                      (screenshot "ERROR-Can-navigate-between-drafts")
                      (throw e)))
                  => (contains THIRD_DRAFT_HTML)))))
