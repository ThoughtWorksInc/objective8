(ns objective8.unit.translation-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.translation :as tr]
            [clojure.algo.generic.functor :as algs]
            [clojure.java.io :as io]))

(def translations-directory "translations")

(defn map->key-set [map]
  (set (keys map)))

(defn map-of-maps->map-of-sets [lang-map]
  (algs/fmap map->key-set lang-map))

(defn check-diff [map-primary map-secondary]
  (first (clojure.data/diff map-primary map-secondary)))

(defn test-languages [en-map other-languages-map]
  (algs/fmap (partial check-diff en-map) other-languages-map))

(facts "translations"
      (let [languages (tr/load-translations (tr/find-translation-resources translations-directory))
            en-map (:en languages)
            other-languages (dissoc languages :en)
            lang-seq (keys other-languages)
            valueless-en-map (map-of-maps->map-of-sets en-map)
            valueless-other-lang-map (algs/fmap map-of-maps->map-of-sets other-languages)
            differences (test-languages valueless-en-map valueless-other-lang-map)]
        differences => (has every? nil?)))

