(ns objective8.functional-tests 
  (:require [midje.sweet :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clj-webdriver.taxi :as wd]
            [objective8.core :as core]
            [objective8.integration-helpers :as integration-helpers]
            [dev-helpers.stub-twitter :refer [stub-twitter-auth-config]]))

(def config-without-twitter (assoc core/app-config :authentication stub-twitter-auth-config))

(defn wait-for-title [title]
  (wd/wait-until #(= (wd/title) title) 5000))

(defn screenshot [filename]
  (wd/take-screenshot :file (str "functional/objective8/screenshots/" 
                                 filename ".png")))

(facts "About user journeys" :functional
       (against-background 
         [(before :contents (do (core/start-server config-without-twitter)
                                (wd/set-driver! {:browser :firefox})))
          (after :contents (do (wd/quit)
                               (integration-helpers/truncate-tables)
                               (core/stop-server)))]
         (fact "can add an objective" :functional  
               (try (wd/to "localhost:8080")
                    (wait-for-title "Objective[8]")
                    (screenshot "01_home_page")

                    (wd/click "a[href='/objectives']") 
                    (wait-for-title "Objectives | Objective[8]")
                    (screenshot "02_objectives_page")

                    (wd/click "a[href='objectives/create']") 
                    (wait-for-title "Sign in or Sign up | Objective[8]")
                    (screenshot "03_sign_in_page")

                    (wd/click "button[title='Sign in with twitter']") 
                    (wait-for-title "Sign up almost there | Objective[8]")
                    (screenshot "04_sign_up_almost_there")

                    (wd/input-text "#username" "funcTestUser123")
                    (-> "#email-address" 
                        (wd/input-text "func_test_user@domain.com")
                        wd/submit) 

                    (screenshot "05_create_objective_page")
                    (wait-for-title "Create an Objective | Objective[8]")

                    (wd/input-text "#objective-title" "Functional test headline")
                    (wd/input-text "#objective-goals" "Functional test goal")
                    (-> "#objective-end-date" 
                        (wd/input-text "2015-12-25")
                        wd/submit) 

                    (wait-for-title "Functional test headline | Objective[8]")
                    (screenshot "06_objective_page")
                    (wd/title)
                    (catch Exception e
                      (screenshot "ERROR") 
                      (throw e)))
               =>  "Functional test headline | Objective[8]"))) 
