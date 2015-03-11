(ns objective8.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]))

(html/defsnippet signed-in-masthead
  "templates/user-navigation/signed-in.html" [[:#clj-user-navigation]] [{:keys [translations user]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translations)
  [:#clj-username] (html/content (:username user)))

