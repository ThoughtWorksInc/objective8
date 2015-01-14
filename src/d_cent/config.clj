(ns d-cent.config)

(def environment (System/getenv))

(def twitter-consumer-token (get environment "TWITTER_CONSUMER_TOKEN"))
(def twitter-consumer-secret-token (get environment "TWITTER_CONSUMER_SECRET_TOKEN"))

(def port (get environment "PORT" "8080"))
