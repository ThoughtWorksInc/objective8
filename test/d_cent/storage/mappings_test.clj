(ns d-cent.storage.mappings-test
  (:require [midje.sweet :refer :all]
            [d-cent.storage.mappings :refer :all]))

(defn json-type? [thing]
    (and (= (type thing) org.postgresql.util.PGobject)
         (= (.getType thing) "json")))

(defn time-type? [thing]
  (= (type thing) java.sql.Timestamp))

(fact "Clojure maps are turned into Postgres JSON types"
      (let [transformed-map (map->json-type {:is "a map" :has "some keys"})]
        transformed-map => json-type?
        (.getValue transformed-map) => "{\"is\":\"a map\",\"has\":\"some keys\"}"))

;; OBJECTIVE
(facts "About map->objective"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [objective (map->objective {:created-by "Mr Bob"
                                              :end-date "2015-01-01T00:01:01Z"})]
               objective => (contains {:created_by "Mr Bob"
                                       :end_date  time-type?
                                       :objective json-type?})
               (str (:end_date objective)) => (contains "2015-01-01 00:01")))
       (fact "throws exception if :created-by or :end-date are missing"
             (map->objective {:a "B"}) => (throws Exception "Could not transform map to objective")
             (map->objective {:a "B" :created-by "Foo"}) => (throws Exception "Could not transform map to objective")
             (map->objective {:a "B" :end-date "Blah"}) => (throws Exception "Could not transform map to objective")))

;;USER
(facts "About map->user"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [user (map->user {:twitter-id "twitter-TWITTERID"})]
               user => (contains {:twitter_id "twitter-TWITTERID"
                                  :user_data json-type?})))
       (fact "throws exception if :twitter-id is missing"
                    (map->user {:a "B"}) => (throws Exception "Could not transform map to user")))

;;COMMENT
(def created-by-id 1234)
(def discussing-id 2345)
(def parent-id 3456)
(facts "About map->comment"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-comment (map->comment {:created-by-id created-by-id
                                          :discussing-id discussing-id
                                          :parent-id parent-id})]
              test-comment => (contains {:created_by_id created-by-id
                                    :discussing_id discussing-id
                                    :parent_id parent-id
                                    :comment json-type?})))
       (fact "throws exception if :created-by-id, :discussing-id or parent-id are missing"
                    (map->comment {:a "B"}) => (throws Exception "Could not transform map to comment")
                    (map->comment {:a "B" :created-by-id created-by-id :discussing-id discussing-id}) => (throws Exception "Could not transform map to comment")
                    (map->comment {:a "B" :created-by-id created-by-id :parent-id parent-id}) => (throws Exception "Could not transform map to comment")
                    (map->comment {:a "B" :discussing-id discussing-id :parent-id parent-id}) => (throws Exception "Could not transform map to comment")))
