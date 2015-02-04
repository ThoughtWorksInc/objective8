(ns d-cent.storage.mappings-test
  (:require [midje.sweet :refer :all]
            [d-cent.storage.mappings :refer :all]))

(defn json-type? [thing]
    (and (= (type thing) org.postgresql.util.PGobject)
         (= (.getType thing) "json")))

(fact "Clojure maps are turned into Postgres JSON types"
      (let [transformed-map (map->json-type {:is "a map" :has "some keys"})] 
        transformed-map => json-type?
        (.getValue transformed-map) => "{\"is\":\"a map\",\"has\":\"some keys\"}"
        ))

(facts "About map->objective"
       (fact "sets the :create_by key"
             (map->objective {:created-by "Mr Bob"}) => (contains {:created_by "Mr Bob"}))
       (fact "sets the :objective key to a json-type of the map"
             (map->objective {:created-by "Mr Bob" :foo "bar"}) => (contains {:objective json-type?}))
       (fact "throws exception if :created-by is missing"
             (map->objective {:a "B"}) => (throws Exception "Could not transform map to objective")))
