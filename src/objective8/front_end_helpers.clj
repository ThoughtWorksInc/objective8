(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn request->draft-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:id :google-doc-html-content])
          (assoc :submitter-id user-id)
          (utils/ressoc :id :objective-id)
          (update-in [:objective-id] #(Integer/parseInt %))
          (utils/ressoc :google-doc-html-content :content)
          (update-in [:content] #(utils/html->hiccup (sanitiser/sanitise-html %)))))
