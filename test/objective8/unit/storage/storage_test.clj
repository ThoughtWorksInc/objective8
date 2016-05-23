(ns objective8.unit.storage.storage_test
  (:require [midje.sweet :refer :all]
            [korma.core :as korma]
            [objective8.back-end.storage.storage :as s]
            [objective8.back-end.storage.mappings :as m]))

(fact "attempts to store an object by looking up the entity mapping"
      (let [some-map {:foo "bar" :entity :i-am-entity}]
        (s/pg-store! some-map) => :the-id
        (provided
          (m/get-mapping some-map) => :fake-entity
          (s/insert :fake-entity anything) => :the-id)))

(fact "attempts to find an object based on a query containing entity"
      (let [some-query {:entity :i-am-entity :foo "bar" :zap "pow"}]
        (s/pg-retrieve some-query) => {:query some-query
                                       :options {}
                                       :result :expected-object }
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo "bar" :zap "pow"} {}) => :expected-object)))

(fact "attempts to update a bearer token for a given bearer name"
      (let [some-update {:entity :bearer-token :bearer-name "name" :bearer-token "new-token"}] 
      (s/pg-update-bearer-token! some-update) => anything
      (provided
        (m/get-mapping some-update) => :fake-entity
        (s/update :fake-entity
                  some-update 
                  {:bearer_name "name"}) => anything)))

(fact "converts hyphens to underscores"
      (let [some-query {:entity :ent :foo-bar "wibble"}]
        (s/pg-retrieve some-query) => {:query some-query :options {} :result :expected-object}
        (provided
          (m/get-mapping some-query) => :fake-entity
          (s/select :fake-entity {:foo_bar "wibble"} {}) => :expected-object)))

(fact "throws exception if no entity key is present"
      (s/pg-retrieve {}) => (throws Exception "Query map requires an :entity key"))

