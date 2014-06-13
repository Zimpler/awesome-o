(ns awesome-o.slack
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [environ.core :refer [env]]))

(defn- post [token payload]
  (http/post "https://pugglepay.slack.com/services/hooks/incoming-webhook"
             {:query-params {:token token}
              :form-params {:payload (json/generate-string payload)}}))

(defn say [& stuff]
  (post (env :slack-general-token "")
        {:text (apply str stuff)
         :username "awesome-o"
         :icon_emoji ":awesomeo:"}))
