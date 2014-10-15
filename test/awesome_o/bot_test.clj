(ns awesome-o.bot-test
  (:require [clojure.test :refer :all]
            [awesome-o.bot :as bot]
            [awesome-o.time :as time]
            [awesome-o.state :as state]))

(defn setup-redis []
  (do (state/flushdb)
      (state/add-person "magnus")
      (state/add-person "jean-louis")
      (state/add-person "patrik")
      (state/set-persons-job "jean-louis" "dev")
      (state/add-location "stockholm")
      (state/add-location "göteborg")))

(deftest integration-test
  (setup-redis)

  (let [user "jean-louis"
        today (time/format-date (time/today))
        reply (fn [text]
                (bot/reply user (str "@awesome-o: " text)))]

    (is (= (reply "anders is a puggle")
           "OK, nice to meet you @anders!"))
    (is (= (reply "who is part of the dev team?")
           "The dev team: jean-louis"))
    (is (= (reply "who is anders?")
           "anders is a puggle"))
    (is (= (reply "anders is a developer")
           "OK, now I know anders is part of the dev team"))
    (is (= (reply "who is part of the dev team?")
           "The dev team: anders, jean-louis"))
    (is (= (reply "who is anders?")
           "anders is a puggle part of the dev team"))
    (is (= (reply "barcelona is a location")
           "OK, now I now that barcelona is a location"))
    (is (= (reply "anders is in barcelona")
           "OK, now I know that anders is in barcelona"))
    (is (= (reply "who is anders?")
           "anders is a puggle part of the dev team located at the barcelona office"))
    (is (= (reply "anders is born on 1980-01-01")
           "OK, now I know anders is born on 1980-01-01"))
    (is (= (reply "when is anders's birthday?")
           "anders was born on 1980-01-01"))
    (is (= (reply "who is slackmaster?")
           "@anders is today's slackmaster"))
    (is (= (reply "anders is away today")
           (str "OK, now I know anders will be away from " today " to " today "
anders was slackmaster but is away, therefore:
@jean-louis is today's slackmaster")))
    (is (= (reply "I'm away today")
           (str "OK, now I know jean-louis will be away from " today " to " today "
jean-louis was slackmaster but is away, therefore:
THERE IS NO DEV! OMG RUN FOR YOUR LIFE!!")))
    (is (= (reply "clear my schedule")
           "OK, I've cleared jean-louis's schedule"))
    (is (= (reply "select next slackmaster")
           "@jean-louis is today's slackmaster"))
    (is (= (reply "what is the meaning of life?")
           "forty-two"))
    (is (= (reply "lisa is a puggle")
           "OK, nice to meet you @lisa!"))
    (is (= (reply "lisa is a ux designer")
           "OK, now I know lisa is part of the ux team"))
    (is (= (reply "who is lisa?")
           "lisa is a puggle part of the ux team"))
    (is (= (reply "johan is a puggle")
           "OK, nice to meet you @johan!"))
    (is (= (reply "johan is a salesman")
           "OK, now I know johan is part of the sales team"))
    (is (= (reply "who is part of the sales team?")
           "The sales team: johan"))
    (is (= (reply "who is johan?")
           "johan is a puggle part of the sales team"))
    (is (= (reply "forget about anders")
           "OK, I've forgotten everything about anders"))
    (is (= (reply "who is part of the dev team?")
           "The dev team: jean-louis"))
    ))
