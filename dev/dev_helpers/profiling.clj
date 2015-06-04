(ns dev-helpers.profiling
  (:require [robert.hooke :as rh]))

(def old-reports (atom []))
(def profile-report (atom {}))

(defn report-call [tag]
  (let [call-times (atom [])] 
    (swap! profile-report assoc tag call-times)
  (fn [f & args]
    (let [start-time (. System (nanoTime))
          result (apply f args)
          end-time (. System (nanoTime))]
      (swap! call-times conj (/ (double (- end-time start-time)) 1000000.0))
      result))))

(defn average [coll]
  (when-not (empty? coll)
  (/ (reduce + coll) (count coll))))

(defn render-report-call [[tag times-atom]]
  (prn tag (apply min @times-atom) (average @times-atom) (apply max @times-atom)))

(defn prn-report []
  (doall
   (->> @profile-report
        (sort-by first)
        (map render-report-call))))

(def vars-with-hooks (atom #{}))

(defn add-hook [target-var f]
  (rh/add-hook target-var f)
  (swap! vars-with-hooks conj target-var))

(defn clear-all-hooks []
  (doseq [v @vars-with-hooks]
    (rh/clear-hooks v)
    (swap! vars-with-hooks disj v)))

(defn show-hooks []
  (doseq [v @vars-with-hooks]
    (prn (str @v " - " (when-let [the-hooks (-> @v meta ::rh/hooks)] @the-hooks)))))

(defn clear [prof-config]
  (clear-all-hooks))

(defn instrument [prof-config]
  (add-hook #'objective8.front-end.handlers/objective-detail (report-call "0 - objective-detail"))
  ;(rh/add-hook #'objective8.front-end.api.http/get-objective (report-call "http api get objective"))
  ;(rh/add-hook #'objective8.front-end.api.http/retrieve-writers (report-call "http api retrieve writers"))
  ;(rh/add-hook #'objective8.front-end.api.http/retrieve-questions (report-call "http api retrieve questions"))
  ;(rh/add-hook #'objective8.front-end.api.http/get-comments (report-call "http api get comments"))
  ;(rh/add-hook #'objective8.front-end.api.http/get-draft (report-call "http api get draft"))
  ;(rh/add-hook #'objective8.back-end.storage.storage/pg-get-objective-as-signed-in-user (report-call "pg get obj as signed in user"))
  ;(rh/add-hook #'objective8.back-end.storage.storage/pg-get-objective (report-call "pg get obj"))
  )
