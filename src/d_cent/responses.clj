(ns d-cent.responses 
  (:require [net.cgrand.enlive-html :as html]
            [clostache.parser :refer [render-resource]]))

(defn simple-response [text]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body text})

;;TODO - better template sturcturing - use functions?


; (defn rendered-response
;   ([template-name]
;    (rendered-response template-name {}))
;   ([template-name vars]
;    (simple-response (render-resource "templates/base.mustache" vars {:page (slurp (str "resources/templates/" template-name))}))))


(defn render-template [template & args]
  (apply str (apply template args)))

(html/deftemplate base "templates/base.html" 
                  [{:keys [title content]}]
                  [:title] (html/content title)
                  [:#main-content] (html/content content))

(html/defsnippet index-page "templates/index.html" [[:a]] [])

(defn rendered-response [template-name]
  (simple-response (render-template base {:title "MY TITLE"
                                          :content (index-page)})))
