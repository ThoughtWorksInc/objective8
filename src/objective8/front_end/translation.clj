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

(defn- csv-line->dictionary-path* [parsed-csv-line]
  (map (comp keyword string/trim) (pop parsed-csv-line)))

(defn csv-line->dictionary-path [parsed-csv-line]
  (let [path (csv-line->dictionary-path* parsed-csv-line)
        path-length (count path)]
    (cond
      (= path-length 2) path
      (> path-length 2) (throw (ex-info "path too long" {:cause :long-path}))
      (< path-length 2) (throw (ex-info "path too short" {:cause :short-path})))))

(def csv-line->dictionary-content peek)

(defn csv-file-name->language-keyword [file-name]
  (keyword (first (string/split file-name #"\.csv$"))))

(defn translation-resource-locator [filename]
  (fn [] {:resource-name (csv-file-name->language-keyword filename)
          :resource (io/reader (io/file "resources" "translations" filename))}))

(def parse-error-messages
  {:long-path "Translation lookup path too long"
   :short-path "Translation lookup path too short"})

(defn- load-translation* [resource-name resource]
  (try
    (let [language-identifier (keyword resource-name)
          dictionary (->> (lazy-read-csv resource)
                          (map (juxt csv-line->dictionary-path
                                     csv-line->dictionary-content))
                          (reduce (fn [d [ks v]] (assoc-in d ks v)) {}))]
      {:status ::success :result {language-identifier dictionary}})
    (catch Exception e
      (let [{cause :cause} (ex-data e)]
        {:status ::parse-error
         :message (parse-error-messages cause)
         :resource-name resource-name}))
    (finally
      (.close resource))))

(defn load-translation [resource-locator]
  (let [{:keys [resource-name resource]} (resource-locator)
        {status :status :as load-result} (load-translation* resource-name resource)]
    load-result))

(defn translation-file? [file]
   (re-matches #".*\.csv$" (.getName file)))

(defn find-translation-resources
  ([path]
   (find-translation-resources path translation-resource-locator))

  ([path resource-locator-fn]
   (->> (io/file path)
        file-seq
        (filter translation-file?)
        (map (comp resource-locator-fn #(.getName %))))))

(defn load-translations [resource-locators]
  (let [load-results (->> resource-locators
                          (map load-translation))
        errors (->> load-results
                    (filter #(= (:status %) ::parse-error)))]
    (if (empty? errors)
      (reduce merge {} (map :result load-results))
      (throw
       (ex-info "Errors when loading translation resources"
                {:causes errors})))))

(def translations-directory "resources/translations")

(defn configure-translations []
  {:dictionary (load-translations (find-translation-resources translations-directory))
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})
