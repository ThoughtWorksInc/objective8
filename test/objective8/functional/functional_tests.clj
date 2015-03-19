(ns objective8.functional.functional-tests 
  (:require [midje.sweet :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clj-webdriver.taxi :as wd]
            [clj-webdriver.core :as wc]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [hiccup.core :as hc]
            [objective8.core :as core]
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

(def SOME_MARKDOWN  "A heading\n===\nSome content")
(def SOME_HTML (hc/html (eh/to-hiccup (ec/mp SOME_MARKDOWN))))

(facts "About user journeys" :functional
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

                    (wd/click "button[title='Sign in with twitter']") 
                    (wait-for-title "Sign up almost there | Objective[8]")
                    (screenshot "sign_up_almost_there")

                    (wd/input-text "#username" "funcTestUser123")
                    (-> "#email-address" 
                        (wd/input-text "func_test_user@domain.com")
                        wd/submit) 

                    (screenshot "create_objective_page")
                    (wait-for-title "Create an Objective | Objective[8]")

                    (wd/input-text "#objective-title" "Functional test headline")
                    (wd/input-text "#objective-goals" "Functional test goal")
                    (-> "#objective-end-date" 
                        (wd/input-text "2015-12-25")
                        wd/submit) 

                    (wait-for-title "Functional test headline | Objective[8]")
                    (swap! journey-state assoc :objective-url (wd/current-url))
                    (screenshot "objective_page")

                    (wd/title)
                    
                    (catch Exception e
                      (screenshot "ERROR-Can-add-an-objective") 
                      (throw e)))
               =>  "Functional test headline | Objective[8]") 

         (fact "Can invite a writer"
               (try (wd/to (:objective-url @journey-state))
                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "objective_page")

                    (wd/click ".func-invite-writer")
                    (wait-for-title "Candidate policy writers | Objective[8]")
                    (screenshot "candidate_writers_page")

                    (wd/input-text "#writer-name" "Functional test writer name")
                    (-> "#reason"
                        (wd/input-text "Functional test invitation reason")
                        wd/submit)
                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "objective_with_invitation_flash")

                    (->> (wd/text ".func-flash-bar")
                         (re-find #"http://.*$")
                         (swap! journey-state assoc :invitation-url))
                    {:page-title (wd/title)
                     :flash-message (wd/text ".func-flash-bar")}

                    (catch Exception e
                      (screenshot "ERROR-Can-invite-a-writer")
                      (throw e)))
               => (contains {:page-title "Functional test headline | Objective[8]"
                             :flash-message (contains "Your invited writer can accept their invitation")}))

         (fact "Can accept a writer invitation"
               (try
                 (wd/to (:invitation-url @journey-state))
                 (wait-for-title "Invitation to draft | Objective[8]")
                 (screenshot "invitation_url")

                 (wd/submit "#clj-invitation-response-accept")
                 (wait-for-title "Candidate policy writers | Objective[8]")
                 (screenshot "candidate_writers_as_recently_added_writer")

                 (wd/text "span.candidate-name")

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
                   (wd/to (str (:objective-url @journey-state) "/drafts/current"))
                   (wait-for-title "Policy draft | Objective[8]")
                   (screenshot "current_draft_no_draft")

                   (wd/click "#clj-add-a-draft") 
                   (wait-for-title "Edit draft | Objective[8]")                 
                   (screenshot "edit_draft_empty")

                   (wd/input-text "#clj-edit-draft-content" SOME_MARKDOWN)
                   (wd/click "button[value='preview']")
                   (wait-for-title "Edit draft | Objective[8]")
                   (screenshot "preview_draft")

                   (wd/click "button[value='submit']")

                   (wait-for-title "Policy draft | Objective[8]")
                   (screenshot "submitted_draft")

                   {:page-title (wd/title)
                    :page-source (wd/page-source)}

                   (catch Exception e
                     (screenshot "ERROR-Can-submit-a-draft")
                     (throw e)))
                 => (contains {:page-title "Policy draft | Objective[8]"
                               :page-source (contains SOME_HTML)})) 

           (fact "Can view current draft"
                 (try
                   (wd/to (:objective-url @journey-state))
                   (wait-for-title "Functional test headline | Objective[8]")
                   (screenshot "drafting_started_objective")

                   (wd/click ".clj-objective-drafting-link")
                   (wait-for-title "Policy draft | Objective[8]")
                   (screenshot "current_draft")

                   {:page-title (wd/title)
                    :page-source (wd/page-source)}

                   (catch Exception e
                     (screenshot "ERROR-Can-view-current-draft")
                     (throw e)))
                 => (contains {:page-title "Policy draft | Objective[8]"
                               :page-source (contains SOME_HTML)})))))
