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

(def ^:private pingify (partial str "@"))

(defn- monday-announcements []
  (let [devs (shuffle (state/available-devs))
        gbgs (shuffle (state/get-available-people-in-location "göteborg"))
        meeting-master (pingify (rand-nth gbgs))
        honeybadgers (->> devs (take 2) (map pingify) (string/join ", "))]
    (say (str "Honeydager monday! ping: " honeybadgers) :channel "dev")
    (say (str "Todays meeting master for dev this week is " meeting-master))))

(defn random-meeting []
  (let [gbgs (state/get-available-people-in-location "göteborg")
        gbg (pingify (rand-nth gbgs))
        sthlms (state/get-available-people-in-location "stockholm")
        sthlm (pingify (rand-nth sthlms))]
    (say (str "Today's random meeting is between " gbg " and " sthlm))))

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
    (when (time/monday-today?) monday-announcements)
    (random-meeting)))
