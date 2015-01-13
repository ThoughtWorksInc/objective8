(ns responses 
  (:require [selmer.parser :refer [render-file]]))

(defn simple-response [text]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body text})

(defn rendered-response
  ([template-name]
   (rendered-response template-name {}))
  ([template-name vars]
   (simple-response(render-file (str "templates/" template-name) vars)))) 
