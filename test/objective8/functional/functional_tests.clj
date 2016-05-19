(ns objective8.functional.functional-tests
  (:require [midje.sweet :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clj-webdriver.taxi :as wd]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [objective8.core :as core]
            [objective8.back-end.domain.users :as users]
            [objective8.integration.integration-helpers :as integration-helpers]
            [dev-helpers.stub-twitter-and-stonecutter :refer :all]
            [objective8.front-end.templates.sign-in :as sign-in]))

(def config-without-twitter-or-stonecutter (assoc core/app-config :authentication stub-twitter-and-stonecutter-auth-config))

(defn wait-for-title [title]
  (try
    (wd/wait-until #(= (wd/title) title) 5000)
    (catch Exception e
      (log/info (str ">>>>>>>>>>> Title never appeared:"))
      (log/info (str "Expected: " title))
      (log/info (str "Actual: " (wd/title)))
      (throw e))))

(def not-empty? (comp not empty?))

(defn wait-for-element [q]
  (try
    (wd/wait-until #(not-empty? (wd/elements q)) 5000)
    (catch Exception e
      (log/info (str "Could not find element: " q))
      (throw e))))

(defn wait-for [pred]
  (try
    (wd/wait-until pred 5000)
    (catch Exception e
      (log/info (str "Waiting for predicate failed"))
      (throw e))))

;(defn not-present? [element]
;  (try
;    (wd/present? element)
;    (catch Exception e
;      (throw e)
;      )))


(defn check-not-present [element]
  (if (try
        (wd/present? element)
        false
        (catch Exception e
          true))
    true
    (do (throw (Throwable. (str "Element " element " was found that should not be present")))
        false)))

(def screenshot-directory "test/objective8/functional/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [filename]
  (log/info (str "Screenshot: " filename))
  (wd/take-screenshot :file (str screenshot-directory "/"
                                 (format "%02d" (swap! screenshot-number + 1))
                                 "_" filename ".png")))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))

(def journey-state (atom nil))
(def test-data-collector (atom {}))

(def objective-description "Functional test description with lots of hipster-ipsum:
                           Master cleanse squid nulla, ugh kitsch biodiesel cronut food truck. Nostrud Schlitz tempor farm-to-table skateboard, wayfarers adipisicing Pitchfork sunt Neutra brunch four dollar toast forage placeat. Fugiat lo-fi sed polaroid Portland et tofu Austin. Blue Bottle labore forage, in bitters incididunt ugh delectus seitan flannel. Mixtape migas cardigan, quis American Apparel culpa aliquip cupidatat et nisi scenester. Labore sriracha Etsy flannel XOXO. Normcore selvage do vero keytar synth.")

(def FIRST_DRAFT_MARKDOWN "First draft heading\n===\n\n- Some content")
(def SECOND_DRAFT_MARKDOWN "Second draft heading\n===\n\n- Some content\n- Some more content")
(def THIRD_DRAFT_MARKDOWN "Third draft heading\n===\n\n- Some content\n- Some more content\n- Another line of content")

(def ADMIN_AND_WRITER_STONECUTTER_ID "d-cent-123123")
(def OBJECTIVE_OWNER_TWITTER_ID "twitter-789789")

(against-background
  [(sign-in/twitter-credentials-present?) => true
   (sign-in/stonecutter-credentials-present?) => true
   (before :contents (do (integration-helpers/db-connection)
                         (integration-helpers/truncate-tables)
                         (core/start-server config-without-twitter-or-stonecutter)
                         (wd/set-driver! {:browser :firefox})
                         (users/store-admin! {:auth-provider-user-id ADMIN_AND_WRITER_STONECUTTER_ID})
                         (reset! journey-state {})
                         (clear-screenshots)))
   (before :facts (reset! test-data-collector {}))
   (after :contents (do (wd/quit)
                        (integration-helpers/truncate-tables)
                        (core/stop-back-end-server)
                        (core/stop-front-end-server)))]


  (fact "can add an objective"
        (try (reset! twitter-id OBJECTIVE_OWNER_TWITTER_ID)
             (wd/to "localhost:8080")
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

             (-> (wd/input-text ".func--input-objective-title" "F")
                 (wd/submit))
             (wait-for-element ".func--objective-title-error")
             (screenshot "create_objective_invalid_title")

             (wd/input-text ".func--input-objective-title" "unctional test headline")
             (-> ".func--input-objective-background"
                 (wd/input-text objective-description)
                 wd/submit)

             (wait-for-title "Functional test headline | Objective[8]")
             (swap! journey-state assoc :objective-url (wd/current-url))
             (screenshot "objective_page")

             {:page-title  (wd/title)
              :modal-text  (wd/text ".func--share-objective-modal-text")
              :writer-name (wd/text ".func--writer-name")}

             (catch Exception e
               (screenshot "ERROR-Can-add-an-objective")
               (throw e)))
        => (contains {:page-title  "Functional test headline | Objective[8]"
                      :modal-text  "Functional test headline"
                      :writer-name "funcTestUser123"}))

  (future-fact "user cannot see empty promoted objectives container"
        (try (wd/click "a[href='/objectives']")

             (wait-for-title "Objectives | Objective[8]")

             ;(check-not-present ".clj-promoted-objectives-container")

             (catch Exception e
               (screenshot "ERROR-Cannot-see-empty-promoted-objectives")
               (throw e))))

  (fact "Can star an objective"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")

             (wd/click ".func--objective-star")

             (wd/to "/")

             (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")

             (screenshot "objective_page_with_starred_objective")

             (wd/attribute ".func--objective-star" :class)

             (catch Exception e
               (screenshot "ERROR-Can-star-an-objective")
               (throw e)))
        => (contains "starred"))

  (fact "Can comment on an objective"
        (try
          (wd/to (:objective-url @journey-state))
          (wait-for-title "Functional test headline | Objective[8]")
          (wd/input-text ".func--comment-form-text-area" "Functional test comment text")
          (wd/click ".func--comment-form-submit")
          (wait-for-title "Functional test headline | Objective[8]")
          (screenshot "objective_with_comment")

          (wait-for-title "Functional test headline | Objective[8]")
          (wd/input-text ".func--comment-form-text-area" "Functional test second comment text")
          (wd/click ".func--comment-form-submit")
          (wait-for-title "Functional test headline | Objective[8]")
          (screenshot "objective_with_two_comments")

          (wd/page-source)

          (catch Exception e
            (screenshot "ERROR-Can-comment-on-an-objective")
            (throw e)))
        => (contains "Functional test comment text"))

  (fact "Can view and navigate comment history for an objective"
        (try
          (wd/to (str (:objective-url @journey-state) "/comments?offset=1"))
          (wait-for-title "Comments for Functional test headline | Objective[8]")
          (screenshot "objective_comment_history_offset_1")
          (swap! test-data-collector assoc :offset-equals-1-comment-count (count (wd/elements ".func--comment-text")))

          (wd/click ".func--previous-link")

          (wait-for-title "Comments for Functional test headline | Objective[8]")
          (screenshot "objective_comment_history_offset_0")
          (swap! test-data-collector assoc :offset-equals-0-comment-count (count (wd/elements ".func--comment-text")))

          @test-data-collector
          (catch Exception e
            (screenshot "ERROR-Can-view-comment-history-for-an-objective")
            (throw e)))
        ) => {:offset-equals-1-comment-count 1
              :offset-equals-0-comment-count 2}

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

             {:modal-text (wd/text ".func--share-question-modal-text")}

             (catch Exception e
               (screenshot "Error-Can-add-questions")
               (throw e)))
        => {:modal-text "Functional test question"})

  (fact "Objective owner can promote and demote questions"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-element ".func--demote-question")
             (wd/click ".func--demote-question")
             (wait-for-element ".func--promote-question")
             (screenshot "demoted_question")

             (wait-for-element ".func--promote-question")
             (wd/click ".func--promote-question")
             (screenshot "promoted_question")


             (catch Exception e
               (screenshot "Error-Objective-owner-can-promote-and-demote-questions")
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


             (wd/input-text ".func--writer-name" "Invitee name")
             (wd/input-text ".func--writer-email" "func_test_writer@domain.com")
             (-> ".func--writer-reason"
                 (wd/input-text "Functional test invitation reason")
                 wd/submit)
             (wait-for-title "Functional test headline | Objective[8]")
             (screenshot "objective_with_invitation_flash")

             (->> (wd/value ".func--invitation-url")
                  (re-find #"http://.*$")
                  (swap! journey-state assoc :invitation-url))
             {:page-title    (wd/title)
              :flash-message (wd/text ".func--invitation-guidance")}

             (catch Exception e
               (screenshot "ERROR-Can-invite-a-writer")
               (throw e)))
        => (contains {:page-title    "Functional test headline | Objective[8]"
                      :flash-message (contains "Your writer's invitation")}))



  (fact "Can accept a writer invitation"
        (try (reset! stonecutter-id ADMIN_AND_WRITER_STONECUTTER_ID)
             (wd/click ".func--masthead-sign-out")
             (screenshot "after_sign_out")

             (wd/to (:invitation-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")
             (screenshot "objective_page_after_hitting_invitation_url")

             (wd/click ".func--sign-in-to-accept")

             (wait-for-title "Sign in or Sign up | Objective[8]")
             (wd/click ".func--sign-in-with-d-cent")
             (wait-for-title "Sign up | Objective[8]")
             (-> "#username"
                 (wd/input-text "funcTestWriter")
                 wd/submit)

             (wait-for-title "Functional test headline | Objective[8]")
             (screenshot "objective_page_after_signing_up")

             (wd/click ".func--invitation-accept")

             (wait-for-title "Create profile | Objective[8]")
             (screenshot "create_profile_page")

             (wd/input-text ".func--name" "Invited writer real name")
             (-> ".func--biog"
                 (wd/input-text "Biography with lots of text...")
                 wd/submit)

             (wait-for-title "Functional test headline | Objective[8]")

             (screenshot "objective_page_from_recently_added_writer")

             (wd/text (second (wd/elements ".func--writer-name")))

             (catch Exception e
               (screenshot "ERROR-Can-accept-a-writer-invitation")
               (throw e)))
        => "Invited writer real name")

  (fact "Can view writer profile page"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")

             (wd/click (second (wd/elements ".func--writer-name")))
             (wait-for-title "Invited writer real name | Objective[8]")
             (screenshot "writer_profile_page")

             {:biog-text       (wd/text (first (wd/elements ".func--writer-biog")))
              :objective-title (wd/text (first (wd/elements ".func--objective-title")))}
             (catch Exception e
               (screenshot "ERROR-can-view-writer-profile")
               (throw e)))
        => {:biog-text       "Biography with lots of text..."
            :objective-title "Functional test headline"})

  (fact "Can edit writer profile"
        (try (wd/click ".func--edit-profile")
             (wait-for-title "Edit profile | Objective[8]")
             (screenshot "edit_profile_page")

             (-> ".func--name"
                 wd/clear
                 (wd/input-text "My new real name"))
             (-> ".func--biog"
                 wd/clear
                 (wd/input-text "My new biography")
                 wd/submit)

             (wait-for-title "My new real name | Objective[8]")
             (screenshot "updated_profile_page")

             {:name (wd/text (first (wd/elements ".func--writer-name")))
              :biog (wd/text (first (wd/elements ".func--writer-biog")))}

             (catch Exception e
               (screenshot "ERROR-can-edit-writer-profile")
               (throw e)))
        => (contains {:biog "My new biography"}))

  (fact "Can access dashboard from profile page"
        (try
          (wd/click ".func--dashboard-link")
          (wait-for-title "Writer dashboard | Objective[8]")
          (screenshot "writer_dashboard_from_profile_page")

          (wd/current-url)

          (catch Exception e
            (screenshot "ERROR-can-access-dashboard-from-profile-page")
            (throw e)))
        => (contains (str (:objective-url @journey-state) "/dashboard/questions")))


  (fact "Can submit a draft"
        (try
          (wd/to (str (:objective-url @journey-state) "/drafts"))
          (wait-for-title "Drafts | Objective[8]")
          (screenshot "drafts_list_no_drafts")

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

          {:page-title  (wd/title)
           :page-source (wd/page-source)}

          (catch Exception e
            (screenshot "ERROR-Can-submit-a-draft")
            (throw e)))
        => (contains {:page-title  "Policy draft | Objective[8]"
                      :page-source (contains "First draft heading")}))

  (fact "Can view latest draft"
        (try
          (wd/to (:objective-url @journey-state))
          (wait-for-title "Functional test headline | Objective[8]")
          (screenshot "objective_with_a_draft")

          (wd/click ".func--drafting-message-link")
          (wait-for-title "Drafts | Objective[8]")
          (screenshot "drafts_list_with_one_draft")

          (wd/click ".func--latest-draft-link")
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "latest_draft")

          {:page-title  (wd/title)
           :page-source (wd/page-source)}

          (catch Exception e
            (screenshot "ERROR-Can-view-latest-draft")
            (throw e)))
        => (contains {:page-title  "Policy draft | Objective[8]"
                      :page-source (contains "First draft heading")}))

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
          (screenshot "list_of_drafts_with_one_draft")

          (wd/click ".func--add-a-draft")
          (wait-for-title "Add draft | Objective[8]")
          (wd/input-text ".func--add-draft-content" SECOND_DRAFT_MARKDOWN)

          (wd/click ".func--submit-action")
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "second_draft")

          (wd/click ".func--parent-link")
          (wait-for-title "Drafts | Objective[8]")

          (wd/click ".func--add-a-draft")
          (wait-for-title "Add draft | Objective[8]")
          (wd/input-text ".func--add-draft-content" THIRD_DRAFT_MARKDOWN)

          (wd/click ".func--submit-action")
          (wait-for-title "Policy draft | Objective[8]")

          (wd/to (str (:objective-url @journey-state) "/drafts"))
          (wait-for-title "Drafts | Objective[8]")
          (screenshot "list_of_drafts_with_three_drafts")

          (wd/to (str (:objective-url @journey-state) "/drafts/latest"))
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "latest_draft_with_previous_button")

          (wd/click ".func--previous-link")
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "second_draft_with_next")

          (wd/click ".func--next-link")
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "third_draft")

          (swap! journey-state assoc :draft-url (wd/current-url))
          (swap! journey-state assoc :section-label (wd/attribute "h1" :data-section-label))

          (wd/page-source)
          (catch Exception e
            (screenshot "ERROR-Can-navigate-between-drafts")
            (throw e)))
        => (contains "Third draft heading"))

  (fact "Can view draft diffs"
        (try
          (wd/click ".func--what-changed")
          (wait-for-title "Draft changes | Objective[8]")
          (screenshot "draft_diff")

          (catch Exception e
            (screenshot "ERROR-Can-view-draft-diffs")
            (throw e))))

  (fact "Can view draft section"
        (try
          (wd/click ".func--back-to-draft")
          (wait-for-title "Policy draft | Objective[8]")
          (wd/click ".func--annotation-link")
          (wait-for-title "Draft section | Objective[8]")
          (screenshot "draft_section")

          (wd/page-source)
          (catch Exception e
            (screenshot "ERROR-Can-view-draft-section")
            (throw e)))
        => (contains "Third draft heading"))

  (fact "Can annotate a draft section"
        (try
          (wd/input-text ".func--comment-form-text-area" "my draft section annotation")
          (wd/click ".func--comment-reason-select")
          (wd/click ".func--comment-form-submit")
          (wait-for-title "Draft section | Objective[8]")
          (screenshot "draft_section_with_comment")

          {:annotation        (wd/text ".func--comment-text")
           :annotation-reason (wd/text ".func--comment-reason-text")}

          (catch Exception e
            (screenshot "ERROR-Can-annotate-a-draft-section")
            (throw e)))
        => {:annotation        "my draft section annotation"
            :annotation-reason "Section is difficult to understand"})

  (fact "Can view number of annotations on a section"
        (try
          (wd/click ".func--back-to-draft")
          (wait-for-title "Policy draft | Objective[8]")
          (screenshot "draft_with_one_annotation")

          (wd/text ".func--annotation-count")
          (catch Exception e
            (screenshot "ERROR-Can-view-number-of-annotations-on-a-section")
            (throw e)))
        => "1")

  (fact "Can navigate to import from Google Drive"
        (try
          (wd/to (str (:objective-url @journey-state) "/drafts"))
          (wait-for-title "Drafts | Objective[8]")

          (wd/click ".func--import-draft-link")
          (wait-for-title "Import draft | Objective[8]")
          (screenshot "import_draft")

          (wd/click ".func--cancel-link")
          (wait-for-title "Drafts | Objective[8]")
          (screenshot "draft_list")

          (catch Exception e
            (screenshot "ERROR-Can-navigate-to-import-from-Google-Drive")
            (throw e))))

  (fact "Can view questions dashboard"
        (try (wd/to "localhost:8080/users/funcTestUser123")
             (wait-for-title "funcTestUser123 | Objective[8]")
             (screenshot "objectives_list_as_a_writer_with_an_objective")

             (wd/click ".func--dashboard-link")

             (wait-for-title "Writer dashboard | Objective[8]")

             (screenshot "questions_dashboard")

             {:page-title  (wd/title)
              :page-source (wd/page-source)}

             (catch Exception e
               (screenshot "ERROR-can-view-questions-dashboard")
               (throw e)))
        => (contains {:page-title  "Writer dashboard | Objective[8]"
                      :page-source (every-checker (contains "Functional test question")
                                                  (contains "Functional test answer"))}))

  (fact "Can add a writer note to an answer"
        (try (-> ".func--dashboard-writer-note-item-field"
                 (wd/input-text "Functional test writer note on answer")
                 wd/submit)

             (wait-for-title "Writer dashboard | Objective[8]")
             (screenshot "questions_dashboard_with_writer_note")

             (wd/text ".func--writer-note-text")

             (catch Exception e
               (screenshot "ERROR-can-add-a-writer-note-to-an-answer")
               (throw e)))
        => "Functional test writer note on answer")

  (fact "Can view comments dashboard"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")
             (wd/click ".func--writer-dashboard-link")
             (wait-for-title "Writer dashboard | Objective[8]")
             (wd/click ".func--comment-dashboard-link")

             (wait-for-title "Writer dashboard | Objective[8]")
             (screenshot "comments_dashboard")

             {:page-title  (wd/title)
              :page-source (wd/page-source)}

             (catch Exception e
               (screenshot "ERROR-can-view-comments-dashboard")
               (throw e)))
        => (contains {:page-title  "Writer dashboard | Objective[8]"
                      :page-source (contains "Functional test comment text")}))

  (fact "Can add a writer note to a comment"
        (try (-> ".func--dashboard-writer-note-item-field"
                 (wd/input-text "Functional test writer note on comment")
                 wd/submit)

             (wait-for-title "Writer dashboard | Objective[8]")
             (screenshot "comment_dashboard_with_writer_note")

             (wd/text ".func--writer-note-text")

             (catch Exception e
               (screenshot "ERROR-can-add-a-writer-note-to-an-comment")
               (throw e)))
        => "Functional test writer note on comment")

  (fact "Can view writer note on objective comment"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")
             (screenshot "writer_note_on_objective_comment")
             (wd/page-source)

             (catch Exception e
               (screenshot "ERROR-can-view-writer-note-on-objective-comment")
               (throw e)))
        => (contains "Functional test writer note on comment"))

  (fact "Can view annotations dashboard"
        (try (wd/to (:objective-url @journey-state))
             (wait-for-title "Functional test headline | Objective[8]")
             (wd/click ".func--writer-dashboard-link")
             (wait-for-title "Writer dashboard | Objective[8]")
             (wd/click ".func--annotation-dashboard-link")

             (wait-for-title "Writer dashboard | Objective[8]")
             (screenshot "annotations_dashboard")

             {:page-title       (wd/title)
              :page-source      (wd/page-source)
              :annotation-count (wd/text ".func--item-count")}

             (catch Exception e
               (screenshot "ERROR-can-view-annotations-dashboard")
               (throw e)))
        => (contains {:page-title       "Writer dashboard | Objective[8]"
                      :page-source      (contains "my draft section annotation")
                      :annotation-count (contains "1")}))

  (fact "Can add a writer note to an annotation"
        (try (-> ".func--dashboard-writer-note-item-field"
                 (wd/input-text "Functional test writer note on annotation")
                 wd/submit)

             (wait-for-title "Writer dashboard | Objective[8]")
             (screenshot "annotation_dashboard_with_writer_note")

             (wd/text ".func--writer-note-text")

             (catch Exception e
               (screenshot "ERROR-can-add-a-writer-note-to-an-annotation")
               (throw e)))
        => "Functional test writer note on annotation")

  (fact "Annotation writer note appears alongside the annotation"
        (try (wd/to (str (:draft-url @journey-state) "/sections/" (:section-label @journey-state)))
             (wait-for-title "Draft section | Objective[8]")
             (screenshot "draft_section_with_annotation_with_writer_note")
             (wd/page-source)
             (catch Exception e
               (screenshot "ERROR-annotation-writer-note-appears-alongside-the-annotation")
               (throw e)))
        => (contains "Functional test writer note on annotation"))

  (fact "User with admin credentials can promote and demote an objective"
        (let [result (try
                       (wd/click ".func--objectives")
                       (wait-for-title "Objectives | Objective[8]")
                       (screenshot "admins_objectives_page")
                       (check-not-present ".clj-promoted-objectives-container")

                       (wd/click ".func--toggle-promoted-objective-button")
                       (wait-for-title "Objectives | Objective[8]")
                       (wait-for-element ".func--promoted-objectives-container")
                       (screenshot "promoted-objectives-list-page")
                       (wd/click ".func--toggle-promoted-objective-button")
                       (wait-for-title "Objectives | Objective[8]")
                       (screenshot "demoted-objectives-list-page")
                       (check-not-present ".clj-promoted-objectives-container")


                       {:page-title (wd/title)
                        :content    (wd/page-source)}
                       (catch Exception e
                         (screenshot "ERROR-User-with-admin-credentials-can-promote-and-demote-an-objective")
                         (throw e)))]
          (:page-title result) => "Objectives | Objective[8]"))

  (fact "User with admin credentials can remove an objective"
        (let [result (try

                       (wd/click ".func--objectives")
                       (wait-for-title "Objectives | Objective[8]")

                       (wd/click ".func--remove-objective")
                       (wait-for-title "Are you sure? | Objective[8]")
                       (screenshot "admin_removal_confirmation_page")

                       (wd/click ".func--confirm-removal")
                       (wait-for-title "Objectives | Objective[8]")
                       (screenshot "objectives_page_after_removing_the_only_objective")
                       {:page-title (wd/title)
                        :content    (wd/page-source)}

                       (catch Exception e
                         (screenshot "ERROR-User-with-admin-credentials-can-remove-objective")
                         (throw e)))]
          (:page-title result) => "Objectives | Objective[8]"
          (:content result) =not=> (contains "Functional test headline")))

  (fact "Can view the removed objective on the admin-activity page"
        (let [objective-id (-> (:objective-url @journey-state)
                               (string/split #"/")
                               last)
              objective-uri (str "/objectives/" objective-id)]
          (try
            (wd/click ".func--admin-link")
            (wait-for-title "Admin activity | Objective[8]")
            (screenshot "admin_activity_page")

            (wd/page-source)

            (catch Exception e
              (screenshot "ERROR-Can-view-the-removed-objective-on-the-admin-activity-page")
              (throw e)))
          => (contains objective-uri))))
