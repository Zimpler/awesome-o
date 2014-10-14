(ns awesome-o.slack
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [environ.core :refer [env]]))

(defn- post [token payload]
  (http/post "https://pugglepay.slack.com/services/hooks/incoming-webhook"
             {:query-params {:token token}
              :form-params {:payload (json/generate-string payload)}}))

(defn channel->token [channel]
  (case channel
    "general" (env :slack-general-token "")
    "dev" (env :slack-dev-token "")))

(defn say [stuff & {:keys [channel username emoji]}]
  (post (channel->token (or channel "general"))
        {:text stuff
         :username (or username "awesome-o")
         :icon_emoji (or emoji ":awesomeo:")}))
