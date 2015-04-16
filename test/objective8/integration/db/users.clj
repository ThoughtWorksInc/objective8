(ns objective8.integration.db.users
  (:require [midje.sweet :refer :all]
            [objective8.users :as users]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(def profile-data {:name "name" :biog "biography"})

(facts "about updating users" 
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "a user can be updated with a writer profile" 
               (let [user (sh/store-a-user)
                     user-with-profile (assoc user :profile profile-data)]
                 (users/update-user! user-with-profile) => user-with-profile))))
