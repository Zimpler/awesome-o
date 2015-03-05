(ns awesome-o.slack
  (:require [awesome-o.bot :as bot]
            [awesome-o.state :as state]
            [awesome-o.time :as time]
            [awesome-o.http :as http]
            [clojure.string :as string]
            [environ.core :refer [env]]))

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
  (http/post (channel->token (or channel "general"))
             {:text stuff
              :username (or username "awesome-o")
              :icon_emoji (or emoji ":awesomeo:")}))

(defn- select-next-slackmaster
  [& {:keys [changed-from]}]
  (say
   (str
    (when changed-from
      (str changed-from " was slackmaster but is away, therefore:\n"))
    (bot/react [:select-next-slackmaster]))
   :channel "general"))

(defn announcement [user-name text]
  (say (str "@everyone: new announcement from " user-name ":\n"
                  text)))

(defn mention [user-name text]
  (let [slack-master (state/get-slackmaster)
        text-response (bot/reply user-name text)]
    (when (and slack-master (not (state/get-slackmaster)))
      (select-next-slackmaster :changed-from slack-master))
    {:text text-response
     :username "awesome-o"
     :icon_emoji ":awesomeo:"}))

(defn ping []
  (when (and (time/working-hour?)
             (state/acquire-daily-announcement))
    (select-next-slackmaster)
    (doseq [person (state/persons-born-today)]
      (say (format "Today is @%s's birthday! Happy birthday!" person)))
    (when (time/monday-today?)
      (let [devs (shuffle (state/available-devs))
            gbgs (shuffle (state/get-available-people-in-location "göteborg"))
            meeting-master (pingify [(rand-nth gbgs)])
            honeybadgers (->> devs (take 2) pingify)]
        (say (str "Honeydager monday! ping: " honeybadgers) :channel "dev")
        (say (str "Todays meeting master for gothenburg this week is " meeting-master))))))
