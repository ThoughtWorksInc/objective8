(ns main
  (:use [org.httpkit.server :only [run-server]]
        [bidi.ring :only [make-handler]]))

(defonce server (atom nil))

(defn simple-response [text] 
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body text})

(defn index-handler [request] 
  (simple-response "Index handler"))

(defn test-handler [request] 
  (simple-response (str "test handler: " (:id (:params request)))))

(def handlers {:index index-handler
               :test test-handler})

(def app
  (make-handler ["/" {"index.html" :index
                      [:id "/test"] :test}] handlers))

(defn start-server []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (reset! server (run-server app {:port port}))))

(defn -main []
  (start-server)
)

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil))
  )

(defn restart-server []
  (stop-server)
  (start-server))
