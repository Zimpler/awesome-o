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

(defn- select-next-meetingmaster
  [& {:keys [changed-from]}]
  (say
   (str
    (when changed-from
      (str changed-from " was meetingmaster but is away, therefore:\n"))
    (bot/react [:select-next-meetingmaster]))
   :channel "dev"))

(def ^:private pingify (partial str "@"))

(defn- select-honeybadgers []
  (let [honeybadgers (->> (state/draw-people-from-job "dev" :number 3)
                          (map pingify)
                          (string/join ", "))]
    (say (str "Honeybadger Monday & Story Triage! ping: " honeybadgers) :channel "dev")))

(defn- random-meeting []
  (let [gbg (pingify (state/random-person-from-location "göteborg"))
        sthlm (pingify (state/random-person-from-location "stockholm"))]
    (say (str "Today's random meeting is between " gbg " and " sthlm))))

(defn announcement [user-name text]
  (say (str "new announcement from " user-name ":\n"
            text)))

(defn mention [user-name text]
  (let [slack-master    (state/get-slackmaster)
        meeting-master  (state/get-meetingmaster)
        text-response   (bot/reply user-name text)]
    (when (state/away? slack-master)
      (select-next-slackmaster :changed-from slack-master))
    (when (and (state/away? meeting-master) (time/monday-today?))
      (select-next-meetingmaster :changed-from meeting-master))
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
      (select-honeybadgers))
    (when (time/friday-today?)
      (select-next-meetingmaster))
    (when (or (time/monday-today?) (time/wednesday-today?)) (random-meeting))))
