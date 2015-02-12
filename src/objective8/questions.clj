(ns objective8.questions
  (:require [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]))

(defn request->question
  "Returns a map of a question with a user if all parts are in the request. Otherwise returns nil"
  [{:keys [params]}]
  (assoc (select-keys params [:question]) :created-by-id (get (friend/current-authentication) :username)))

; (defn store-comment! [comment]
;  (storage/pg-store! (assoc comment :entity :comment)))
;
;
; (defn retrieve-comments [objective-id]
;   (let [{result :result} (storage/pg-retrieve {:entity :comment :objective-id objective-id})]
;     (map #(dissoc % :entity) result))) ;TODO limit to 50
