(ns d-cent.responses 
  (:require [clostache.parser :refer [render-resource]]))

(defn simple-response [text]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body text})

;;TODO - better template sturcturing - use functions?
(defn rendered-response
  ([template-name]
   (rendered-response template-name {}))
  ([template-name vars]
   (simple-response (render-resource "templates/base.mustache" vars {:page (slurp (str "resources/templates/" template-name))})))) 
