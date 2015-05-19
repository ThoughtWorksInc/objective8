(ns dev-helpers.translation
  (:require [clojure-csv.core :as csv]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [taoensso.tower :as tower]
            [objective8.front-end.translation :as translation]))

(def get-path first)
(def get-content second)

(defn- get-children [[path d]]
  (let [children (seq d)]
    (map (juxt (comp #(conj path %) get-path)
               get-content)
         children)))

(defn- string-from-keyword [kwd]
  (string/replace-first (str kwd) ":" ""))

(defn- walk-translation-dictionary [d]
  (tree-seq (comp map? get-content) get-children [[] d]))

(def content-node? (comp not map? get-content))

(defn- path->tower-key [path]
  (apply keyword (map string-from-keyword path)))

(defn translation-paths [d]
  (->> (walk-translation-dictionary d)
       (filter content-node?)
       (map get-path)))

(defn update-template-for-locale
  "Given a tower translation function t and a locale identifier ---
  i.e. :es, :en, ... --- update the translation csv resource for that
  locale by merging in any missing keys based on the :en resource."
  [locale-identifier]
  (let [{dictionary :dictionary :as config} (translation/configure-translations)
        en-dictionary (:en dictionary)
        target-dictionary (get dictionary locale-identifier {})
        paths (translation-paths en-dictionary)
        content (map (comp flatten
                      (juxt (partial map string-from-keyword)
                            #(get-in target-dictionary %
                                     (get-in en-dictionary % ""))))
                     paths)
        target-file (str "resources/translations/" (string-from-keyword locale-identifier) ".csv")]
    (spit target-file (csv/write-csv content))))

(defn main [& locales]
  (log/info (apply str "Generating locale translation templates for: " (interpose ", " locales)))
  (doall (map (comp update-template-for-locale keyword) locales)))
