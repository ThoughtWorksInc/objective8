(ns objective8.unit.translation-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.translation :as tr]
            [clojure.algo.generic.functor :as algs]))

(def translations-directory "translations")

(defn map->key-set [map]
  (set (keys map)))

(defn map-of-maps->map-of-sets [lang-map]
  (algs/fmap map->key-set lang-map))

(tabular
  (fact "All translation files contain the same entries"
        (let [languages (tr/load-translations (tr/find-translation-resources translations-directory))
              en-map (:en languages)
              lang-map (?language languages)
              valueless-en-map (map-of-maps->map-of-sets en-map)
              valueless-lang-map (map-of-maps->map-of-sets lang-map)
              difference (clojure.data/diff valueless-en-map valueless-lang-map)
              ]
          (first difference) => nil
          ))
  ?language
  :es)

