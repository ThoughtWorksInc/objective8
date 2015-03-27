(ns objective8.storage.uris
  (:require [bidi.bidi :as bidi]))

(def uri-routes
  ["/" {["objectives/" [#"\d+" :objective-id]] {"" :objective
                                                ["/drafts/" [#"\d+" :draft-id]] :draft}}])

(defn uri->query [uri]
  (if-let [{entity :handler route-params :route-params} (bidi/match-route uri-routes uri)]
    (cond
      (= entity :objective) {:entity :objective
                             :_id (Integer/parseInt (:objective-id route-params))}

      (= entity :draft) {:entity :draft
                         :_id (Integer/parseInt (:draft-id route-params))})))
