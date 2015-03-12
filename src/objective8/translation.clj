(ns objective8.translation
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure-csv.core :as csv]
            [taoensso.tower :as tower]))

;; From http://stackoverflow.com/a/19656800/576322
(defn lazy-read-csv
  "Lazily read from a csv file or reader, ensuring it is closed correctly when exhausted"
  [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/parse-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                (if-let [s (seq wrapped)]
                  (cons (first s) (lazy (rest s)))
                  (.close in-file))))]
        (lazy csv-seq)))

(defn translation-resource-locator [language]
  (let [filename (str language ".csv")]
    (fn [] {:resource-name language
            :resource (io/reader (io/file "resources" "translations" filename))})))

(defn csv-line->dictionary-path [parsed-csv-line]
  (map (comp keyword string/trim) (pop parsed-csv-line)))

(defn csv-file-name->language-keyword [file-name]
  (keyword (first (string/split file-name #"\.csv$"))))

(defn load-translation [resource-locator]
  (let [{:keys [resource resource-name]} (resource-locator)
        language-identifier (keyword resource-name)
        dictionary (->> (lazy-read-csv resource)
                        (map (juxt csv-line->dictionary-path peek))
                        (reduce (fn [d [ks v]] (assoc-in d ks v)) {}))]
    {language-identifier dictionary}))

(defn load-translations [resource-locators]
  (->> resource-locators
       (map load-translation)
       (reduce merge {})))

(defn configure-translations []
  {:dictionary (load-translations (map translation-resource-locator ["en" "es"]))
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})
