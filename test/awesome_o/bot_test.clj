(ns awesome-o.bot-test
  (:require
   [clojure.test :refer :all]
   [awesome-o.slack :as slack]
   [awesome-o.http :as http]
   [awesome-o.state :as state]
   [awesome-o.time :as time]))

(def test-user "jean-louis")
(def today (time/format-date (time/today)))

(defn- setup-redis [f]
  (state/flushdb)
  (state/reset-state)
  (state/add-person "magnus")
  (state/add-person test-user)
  (state/add-person "patrik")
  (state/set-birthday "patrik" today)
  (state/set-persons-job "patrik" "dev")
  (state/set-persons-job test-user "dev")
  (state/add-person "kristoffer")
  (state/set-persons-location test-user "göteborg")
  (state/set-persons-location "kristoffer" "stockholm")
  (f))

(def sent-to-slack (atom []))

(defn- rebind-post [f]
  (reset! sent-to-slack [])
  (with-redefs
    [http/post (fn [token payload] (swap! sent-to-slack conj (:text payload)))
     shuffle identity
     rand-nth first]
    (f)))

(defn- mention [text]
  (:text (slack/mention test-user (str "@awesome-o: " text))))

(use-fixtures :each setup-redis rebind-post)

(deftest random-meeting-test
  (let [meeting slack/random-meeting]
    (is (or (= "Today's random meeting is between @kristoffer and @jean-louis")
            (= "Today's random meeting is between @jean-louis and @kristoffer")))))

(deftest mention-test
  (with-redefs
    [time/monday-today? (constantly true)]
    (is (= (mention "anders is a puggle")
           "OK, nice to meet you @anders!"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: jean-louis, patrik"))

    (is (= (mention "who is anders?")
           "anders is a puggle"))

    (is (= (mention "anders is in the dev team")
           "OK, now I know anders is part of the dev team"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: jean-louis, patrik, anders"))

    (is (= (mention "who is anders?")
           "anders is a puggle part of the dev team"))

    (is (= (mention "anders is in göteborg")
           "OK, now I know that anders is in göteborg"))

    (is (= (mention "who is anders?")
           "anders is a puggle part of the dev team located at the göteborg office"))

    (is (= (mention "anders is born on 1980-01-01")
           "OK, now I know anders is born on 1980-01-01"))

    (is (= (mention "patrik was born on 1987-10-09")
           "OK, now I know patrik is born on 1987-10-09"))

    (is (= (mention "I was born on 1987-10-09")
           "OK, now I know jean-louis is born on 1987-10-09"))

    (is (= (mention "when is anders's birthday?")
           "anders was born on 1980-01-01"))

    (is (= (mention "I'm away today")
           (str "OK, now I know jean-louis will be away from " today " to " today)))

    (is (= (mention "patrik is away today")
           (str "OK, now I know patrik will be away from " today " to " today)))

    (is (= (mention "anders is away today")
           (str "OK, now I know anders will be away from " today " to " today)))

    (is (= @sent-to-slack []))

    (is (= (mention "clear my schedule")
           "OK, I've cleared jean-louis's schedule"))

    (is (= (mention "clear anders's schedule")
           "OK, I've cleared anders's schedule"))

    (is (= (mention "what is the meaning of life?")
           "forty-two"))

    (is (= (mention "lisa is a puggle")
           "OK, nice to meet you @lisa!"))

    (is (= (mention "lisa is in the design team")
           "OK, now I know lisa is part of the design team"))

    (is (= (mention "who is lisa?")
           "lisa is a puggle part of the design team"))

    (is (= (mention "johan is a puggle")
           "OK, nice to meet you @johan!"))

    (is (= (mention "johan is in the sales team")
           "OK, now I know johan is part of the sales team"))

    (is (= (mention "who is part of the sales team?")
           "The sales team: johan"))

    (is (= (mention "who is johan?")
           "johan is a puggle part of the sales team"))

    (is (= (mention "forget about anders")
           "OK, I've forgotten everything about anders"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: jean-louis, patrik"))))

(deftest ping-test-non-working
  (testing "non-working hours - does nothing"
    (with-redefs
      [time/working-hour? (constantly false)]
      (slack/ping))
    (is (= [] @sent-to-slack))))

(deftest ping-test-monday
  (testing "a working hour monday - sends all announcements"
    ;; Need more devs to test honeybadger monday
    (state/add-person "hugo")
    (state/set-persons-job "hugo" "dev")
    (state/add-person "gustaf")
    (state/set-persons-job "gustaf" "dev")
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly true)
       time/wednesday-today? (constantly false)
       time/friday-today? (constantly false)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (and (= (drop-last @sent-to-slack)
                ["Today is @patrik's birthday! Happy birthday!"
                 "Honeybadger Monday & Story Triage! ping: @jean-louis, @patrik, @hugo"])
             (re-matches #"Today's random meeting is between @.* and @.*"
                         (last @sent-to-slack))))))

(deftest ping-test-tuesday-thursday
  (testing "tuesday and thursday - does daily announcements"
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly false)
       time/wednesday-today? (constantly false)
       time/friday-today? (constantly false)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (= @sent-to-slack
           ["Today is @patrik's birthday! Happy birthday!"]))))

(deftest ping-test-wednesday
  (testing "wednesday - does random meeting and daily announcements"
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly false)
       time/wednesday-today? (constantly true)
       time/friday-today? (constantly false)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (and (= (first @sent-to-slack)
                "Today is @patrik's birthday! Happy birthday!")
             (re-matches #"Today's random meeting is between @.* and @.*"
                         (last @sent-to-slack))))))

(deftest ping-test-friday
  (testing "friday - does random meeting and daily announcements"
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly false)
       time/wednesday-today? (constantly false)
       time/friday-today? (constantly true)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (and (= (first @sent-to-slack)
                "Today is @patrik's birthday! Happy birthday!")
             (re-matches #"Today's random meeting is between @.* and @.*"
                         (last @sent-to-slack))))))

(deftest schedule-test
  (is (= (mention "what is jean-louis schedule?")
         "I do not have a schedule for jean-louis"))
  (is (not (state/away? "jean-louis")))

  (is (= (mention "jean-louis is away today")
         (str "OK, now I know jean-louis will be away from "
              (time/today) " to "
              (time/today))))

  (is (= (mention "what is jean-louis schedule?")
         (str "jean-louis will be away on " (time/today))))
  (is (state/away? "jean-louis"))
  (is (= (mention "clear jean-louis schedule"))
      "OK, I've cleared jean-louis's schedule")

  (is (= (mention "jean-louis is away tomorrow")
         (str "OK, now I know jean-louis will be away from "
              (time/tomorrow) " to "
              (time/tomorrow))))

  (is (= (mention "what is jean-louis schedule?")
         (str "jean-louis will be away on " (time/tomorrow))))
  (is (not (state/away? "jean-louis")))

  (is (= (mention (str "jean-louis is away until " (time/n-days-from-today 3)))
         (str "OK, now I know jean-louis will be away from "
              (time/today) " to "
              (time/n-days-from-today 2))))

  (is (= (mention "what is jean-louis schedule?")
         (str "jean-louis will be away"
              " on " (time/tomorrow)
              ", from " (time/today) " to " (time/n-days-from-today 2))))
  (is (state/away? "jean-louis"))
  (is (= (mention "clear jean-louis schedule"))
      "OK, I've cleared jean-louis's schedule")

  (is (= (mention (str "jean-louis will be away from "
                       (time/n-days-ago-today 3) " to "
                       (time/n-days-ago-today 1)))
         (str "OK, now I know jean-louis will be away from "
              (time/n-days-ago-today 3) " to "
              (time/n-days-ago-today 1))))

  (is (= (mention "what is jean-louis schedule?")
         "I do not have a schedule for jean-louis"))
  (is (not (state/away? "jean-louis")))

  (is (= (mention (str "jean-louis will be away from "
                       (time/n-days-from-today 2) " to "
                       (time/n-days-from-today 5)))
         (str "OK, now I know jean-louis will be away from "
              (time/n-days-from-today 2) " to "
              (time/n-days-from-today 5))))

  (is (= (mention "what is jean-louis schedule?")
         (str "jean-louis will be away from "
              (time/n-days-from-today 2) " to "
              (time/n-days-from-today 5))))
  (is (not (state/away? "jean-louis"))))
