(ns objective8.unit.translation-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.translation :as tr]
            [clojure.algo.generic.functor :as algs]))

(def translations-directory "translations")

(defn map->key-set [map]
  (set (keys map)))

(defn map-of-maps->map-of-sets [lang-map]
  (algs/fmap map->key-set lang-map))

(defn check-diff [map-primary map-secondary]
  (let [diff (clojure.data/diff map-primary map-secondary)]
    (concat (first diff) (second diff))))

(defn test-languages [en-map other-languages-map]
  (algs/fmap (partial check-diff en-map) other-languages-map))

(facts "the translations have the same keys in all languages"
      (let [languages (tr/load-translations (tr/find-translation-resources translations-directory))
            en-map (:en languages)
            other-languages (dissoc languages :en)
            valueless-en-map (map-of-maps->map-of-sets en-map)
            valueless-other-lang-map (algs/fmap map-of-maps->map-of-sets other-languages)
            differences (test-languages valueless-en-map valueless-other-lang-map)]
        differences => (has every? empty?)))

