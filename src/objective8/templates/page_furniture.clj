(ns objective8.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]))

(html/defsnippet user-navigation-signed-in
  "templates/jade/library.html" [:.clj-user-navigation-signed-in] [{:keys [translations user]}]
  [:.clj-masthead-sign-out] (html/set-attr "title" (translations :navigation-global/sign-out-title))
  [:.clj-masthead-sign-out-text] (html/content (translations :navigation-global/sign-out-text))
  [:.clj-username] (html/content (:username user)))

(html/defsnippet user-navigation-signed-out
  "templates/jade/library.html" [:.clj-user-navigation-signed-out] [{:keys [translations user]}]
  [:.clj-masthead-sign-in] (html/set-attr "title" (translations :navigation-global/sign-in-title))
  [:.clj-masthead-sign-in-text] (html/content (translations :navigation-global/sign-in-text)))
