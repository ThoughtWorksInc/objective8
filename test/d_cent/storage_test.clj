(ns d-cent.storage_test
  (:require [midje.sweet :refer :all]
            [d-cent.storage :as s]))

(fact "generates ids for records"
      (let [saved-record (s/store! "some-collection" {:A "B"})]
        (:_id saved-record) =not=> nil))

(fact "can store and retrieve records in collections"
      (let [saved-record-1 (s/store! "some-collection" {:is "a document"})
            saved-record-2 (s/store! "some-other-collection" {:is "another document"})]
        
        saved-record-1 => (contains {:is "a document"} :in-any-order) 
        saved-record-2 => (contains {:is "another document"} :in-any-order) 

        (s/retrieve "some-collection" (:_id saved-record-1)) => (contains {:is "a document"} :in-any-order)
        (s/retrieve "some-other-collection" (:_id saved-record-2)) => (contains {:is "another document"} :in-any-order)))

(fact "returns nil if a record doesn't exist"
      (s/store! "collection" {:foo "bar"})
      (s/retrieve "collection" "not an id") => nil)

(fact "returns nil if a collection doesn't exist"
      (s/retrieve "does not exist" "not an id") => nil)
