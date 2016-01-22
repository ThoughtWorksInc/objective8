(ns objective8.front-end.templates.draft-diff
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.utils :as utils]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def draft-diff-template (html/html-resource "templates/jade/draft-diff.html" {:parser jsoup/parser}))

(defn draft-diff-page [{:keys [data doc] :as context}]
  (let [{:keys [current-draft previous-draft-diffs current-draft-diffs]} data]
    (html/at draft-diff-template
                [:.clj-masthead-signed-out] nil
                [:.clj-status-bar] nil
                [:.clj-close-link] (html/set-attr :href (utils/path-for :fe/draft :id (:objective-id current-draft) :d-id (:_id current-draft)))
                [:.clj-add-previous-draft] (html/html-content previous-draft-diffs)
                [:.clj-add-current-draft] (html/html-content current-draft-diffs))))
