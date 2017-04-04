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

(def ^:private pingify (partial str "@"))

(defn- select-honeybadgers []
  (let [honeybadgers (->> (state/draw-people-from-job "dev" :number 3)
                          (map pingify)
                          (string/join ", "))]
    (say (str "Honeybadger Monday & Story Triage! ping: " honeybadgers) :channel "dev")))

(defn random-meeting []
  (let [start-loc (state/random-location)
        first-metee (state/random-person-from-location start-loc)
        second-metee (if (= start-loc "remote")
                       (state/random-person-other-than first-metee)
                       (state/random-person-not-from-location start-loc))]
    (say (str "Today's random meeting is between " (pingify first-metee)
              " and " (pingify second-metee)))))

(defn announcement [user-name text]
  (say (str "new announcement from " user-name ":\n"
            text)))

(defn mention [user-name text]
  (let [text-response   (bot/reply user-name text)]
    {:text text-response
     :username "awesome-o"
     :icon_emoji ":awesomeo:"}))

(defn ping []
  (when (and (time/working-hour?)
             (state/acquire-daily-announcement))
    (doseq [person (state/persons-born-today)]
      (say (format "Today is @%s's birthday! Happy birthday!" person)))
    (when (time/monday-today?)
      (select-honeybadgers))
    (when (or (time/monday-today?)
              (time/wednesday-today?)
              (time/friday-today?)) (random-meeting))))
