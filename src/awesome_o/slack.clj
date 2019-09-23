(ns awesome-o.slack
  (:require [awesome-o.bot :as bot]
            [awesome-o.state :as state]
            [awesome-o.time :as time]
            [awesome-o.http :as http]
            [clojure.string :as string]
            [environ.core :refer [env]]))

(defn say [stuff]
  (http/post (env :slack-general-token "")
             {:text stuff
              :link_names 1
              :username "awesome-o"
              :icon_emoji ":awesomeo:"}))

(defn random-meeting []
  (let [first-metee (state/random-person)
        second-metee (state/random-person-from-other-location first-metee)]
    (say (str "Today's random meeting is between " first-metee
              " and " second-metee))))

(defn random-triple-meeting []
  (let [[first-metee second-metee third-metee] (state/three-random-people-from-different-locations)]
    (say (str "Today's random trio meeting is between "
              first-metee
              ", "
              second-metee
              " and "
              third-metee))))

(defn announcement [user-name text]
  (say (str "new announcement from " user-name ":\n"
            text)))

(defn mention [user-name text]
  (let [text-response   (bot/reply user-name text)]
    {:text text-response
     :link_names 1
     :username "awesome-o"
     :icon_emoji ":awesomeo:"}))

(defn ping []
  (when (and (time/working-hour?)
             (state/acquire-daily-announcement))
    (doseq [person (state/persons-born-today)]
      (say (format "Today is %s's birthday! Happy birthday!" person)))
    #_(when (or (time/monday-today?)
                (time/wednesday-today?)
                (time/friday-today?)) (random-meeting))
    #_(when (or (time/tuesday-today?)
                (time/thursday-today?)) (random-triple-meeting))))
