(ns awesome-o.slack
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [awesome-o.bot :as bot]
            [awesome-o.state :as state]
            [awesome-o.time :as time]
            [clojure.string :as string]
            [environ.core :refer [env]]))

(defn- post [token payload]
  (http/post "https://pugglepay.slack.com/services/hooks/incoming-webhook"
             {:query-params {:token token}
              :form-params {:payload (json/generate-string payload)}}))

(defn- channel->token [channel]
  (case channel
    "general" (env :slack-general-token "")
    "dev" (env :slack-dev-token "")))

(defn- pingify
  "takes a list of people and creates
   a string like in the example
   (pingify [\"lars\" \"magnus\"])
   => \"@lars, @magnus\""
  [people]
  (string/join ", " (map (partial str "@") people)))


(defn say [stuff & {:keys [channel username emoji]}]
  (post (channel->token (or channel "general"))
        {:text stuff
         :username (or username "awesome-o")
         :icon_emoji (or emoji ":awesomeo:")}))

(defn announcement [user-name text]
  (say (str "@everyone: new announcement from " user-name ":\n"
                  text)))

(defn mention [user-name text]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string
          {:text (bot/reply user-name text)
           :username "awesome-o"
           :icon_emoji ":awesomeo:"})})

(defn ping []
  (when (and (time/working-hour?)
             (state/acquire-daily-announcement))
    (say (bot/react [:select-next-slackmaster]))
    (doseq [person (state/persons-born-today)]
      (say (format "Today is @%s's birthday! Happy birthday!" person)))
    (when (time/monday-today?)
      (let [devs (shuffle (state/available-devs))
            meeting-master (pingify [(rand-nth devs)])
            honeybadgers (->> devs (take 3) pingify)]
        (say (str "Honeydager monday! ping: " honeybadgers) :channel "dev")
        (say (str "Todays meeting master for dev this week is " meeting-master))))))
