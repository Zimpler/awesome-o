(ns awesome-o.bot-test
  (:require
   [clojure.test :refer :all]
   [awesome-o.slack :as slack]
   [awesome-o.http :as http]
   [awesome-o.state :as state]
   [awesome-o.time :as time]))

(def test-user "<@test-user>")

(def today (time/format-date (time/today)))

(defn- mock-redis [f]
  (let [mock-state (atom {})]
    (with-redefs
      [state/flushdb      #(reset! mock-state {})
       state/reset-state  #(reset! mock-state {:persons {}})
       state/get-state     (fn [] @mock-state)
       state/update-state #(swap! mock-state %)]
      (f))))

(defn- setup-test-state-data [f]
  (state/flushdb)
  (state/reset-state)
  (state/add-person "<@user1>")
  (state/add-person test-user)
  (state/add-person "<@user2>")
  (state/set-birthday "<@user2>" today)
  (state/set-persons-job "<@user2>" "dev")
  (state/set-persons-job test-user "dev")
  (state/add-person "<@user3>")
  (state/set-persons-location test-user "göteborg")
  (state/set-persons-location "<@user3>" "stockholm")
  (f))

(def sent-to-slack (atom []))

(defn does-mention-users [search-str names]
  (every? (fn [name] (.contains search-str name)) names))

(defn- rebind-post [f]
  (reset! sent-to-slack [])
  (with-redefs
    [http/post (fn [token payload] (swap! sent-to-slack conj (:text payload)))
     shuffle identity
     rand-nth first]
    (f)))

(defn- mention [text]
  (:text (slack/mention test-user (str "bot: " text))))

(use-fixtures :each mock-redis setup-test-state-data rebind-post)

(deftest random-meeting-test
  (state/remove-person "<@user1>")
  (state/remove-person "<@user2>")
  (let [meeting (slack/random-meeting)]
    (is (or (= meeting ["Today's random meeting is between <@user3> and <@test-user>"])
            (= meeting ["Today's random meeting is between <@test-user> and <@user3>"])))))

(deftest random-triple-meeting-test
  (testing "A random meeting between three people from different locations"
    (let [location-of-people {"<@user1>"     "göteborg"
                              "<@test-user>" "göteborg"
                              "<@user4>"     "berlin"
                              "<@user5>"     "berlin"
                              "<@user6>"     "remote"
                              "<@user7>"     "remote"
                              "<@user8>"     "stockholm"
                              "<@user3>"     "stockholm"}]
      (state/flushdb)
      (state/reset-state)
      (doseq [person (keys location-of-people)]
        (state/add-person person)
        (state/set-persons-location person (get location-of-people person)))
      (let [meeting-participants (state/three-random-people-from-different-locations)
            location-of-participants (map #(get location-of-people %) meeting-participants)]
        (is (= 3 (count meeting-participants)))
        (is (= location-of-participants (distinct location-of-participants))))))

  (testing "Slack message for random trio meeting pings three people"
    (let [location-of-people {"<@user1>" "göteborg"
                              "<@user4>" "berlin"
                              "<@user6>" "remote"
                              "<@user8>" "stockholm"}]
      (state/flushdb)
      (state/reset-state)
      (doseq [person (keys location-of-people)]
        (state/add-person person)
        (state/set-persons-location person (get location-of-people person)))
      (is (= 3 (->> (slack/random-triple-meeting)
                    first
                    (re-seq #"@")
                    count))))))

(deftest mention-test
  (with-redefs
    [time/monday-today? (constantly true)]
    (is (= (mention "<@user10> is a puggle")
           "OK, nice to meet you <@user10>!"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: <@test-user>, <@user2>"))

    (is (= (mention "who is <@user10>?")
           "<@user10> is a puggle"))

    (is (= (mention "<@user10> is in the dev team")
           "OK, now I know <@user10> is part of the dev team"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: <@test-user>, <@user2>, <@user10>"))

    (is (= (mention "who is <@user10>?")
           "<@user10> is a puggle part of the dev team"))

    (is (= (mention "<@user10> is in göteborg")
           "OK, now I know that <@user10> is in göteborg"))

    (is (= (mention "who is <@user10>?")
           "<@user10> is a puggle part of the dev team located at the göteborg office"))

    (is (= (mention "<@user10> is born on 1980-01-01")
           "OK, now I know <@user10> is born on 1980-01-01"))

    (is (= (mention "<@user2> was born on 1987-10-09")
           "OK, now I know <@user2> is born on 1987-10-09"))

    (is (= (mention "I was born on 1987-10-09")
           "OK, now I know <@test-user> is born on 1987-10-09"))

    (is (= (mention "when is <@user10>'s birthday?")
           "<@user10> was born on 1980-01-01"))

    (is (= (mention "I'm away today")
           (str "OK, now I know <@test-user> will be away from " today " to " today)))

    (is (= (mention "<@user2> is away today")
           (str "OK, now I know <@user2> will be away from " today " to " today)))

    (is (= (mention "<@user10> is away today")
           (str "OK, now I know <@user10> will be away from " today " to " today)))

    (is (= @sent-to-slack []))

    (is (= (mention "clear my schedule")
           "OK, I've cleared <@test-user>'s schedule"))

    (is (= (mention "clear <@user10>'s schedule")
           "OK, I've cleared <@user10>'s schedule"))

    (is (= (mention "what is the meaning of life?")
           "forty-two"))

    (is (= (mention "<@user9> is a puggle")
           "OK, nice to meet you <@user9>!"))

    (is (= (mention "<@user9> is in the design team")
           "OK, now I know <@user9> is part of the design team"))

    (is (= (mention "who is <@user9>?")
           "<@user9> is a puggle part of the design team"))

    (is (= (mention "<@user8> is a puggle")
           "OK, nice to meet you <@user8>!"))

    (is (= (mention "<@user8> is in the sales team")
           "OK, now I know <@user8> is part of the sales team"))

    (is (= (mention "who is part of the sales team?")
           "The sales team: <@user8>"))

    (is (= (mention "who is <@user8>?")
           "<@user8> is a puggle part of the sales team"))

    (is (= (mention "forget about <@user10>")
           "OK, I've forgotten everything about <@user10>"))

    (is (= (mention "who is part of the dev team?")
           "The dev team: <@test-user>, <@user2>"))))

(deftest ping-test-non-working
  (testing "non-working hours - does nothing"
    (with-redefs
      [time/working-hour? (constantly false)]
      (slack/ping))
    (is (= [] @sent-to-slack))))

(deftest ping-test-monday
  (testing "a working hour monday - sends all announcements"
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly true)
       time/wednesday-today? (constantly false)
       time/friday-today? (constantly false)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (and (= (first @sent-to-slack)
                "Today is <@user2>'s birthday! Happy birthday!")
             #_(re-matches #"Today's random meeting is between @.* and @.*"
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
           ["Today is <@user2>'s birthday! Happy birthday!"]))))

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
                "Today is <@user2>'s birthday! Happy birthday!")
             #_(re-matches #"Today's random meeting is between @.* and @.*"
                           (last @sent-to-slack))))))

(deftest ping-test-friday
  (testing "friday - does random meeting and daily announcements"
    (state/remove-person test-user)
    (state/remove-person "<@user3>")
    (state/remove-person "<@user1>")
    (state/add-person "<@user11>")
    (state/set-persons-location "<@user11>" "remote")
    (state/set-persons-location "<@user2>" "remote")
    (with-redefs
      [time/working-hour? (constantly true)
       time/monday-today? (constantly false)
       time/wednesday-today? (constantly false)
       time/friday-today? (constantly true)
       state/acquire-daily-announcement (constantly true)]
      (slack/ping))
    (is (and (= (first @sent-to-slack)
                "Today is <@user2>'s birthday! Happy birthday!")
             #_(does-mention-users (last @sent-to-slack)
                                   ["<@user11>" "<@user2>"])))))

(deftest schedule-test
  (is (= (mention "what is <@test-user> schedule?")
         "I do not have a schedule for <@test-user>"))
  (is (not (state/away? "<@test-user>")))

  (is (= (mention "<@test-user> is away today")
         (str "OK, now I know <@test-user> will be away from "
              (time/today) " to "
              (time/today))))

  (is (= (mention "what is <@test-user> schedule?")
         (str "<@test-user> will be away on " (time/today))))
  (is (state/away? "<@test-user>"))
  (is (= (mention "clear <@test-user> schedule")
         "OK, I've cleared <@test-user>'s schedule"))

  (is (= (mention "<@test-user> is away tomorrow")
         (str "OK, now I know <@test-user> will be away from "
              (time/tomorrow) " to "
              (time/tomorrow))))

  (is (= (mention "what is <@test-user> schedule?")
         (str "<@test-user> will be away on " (time/tomorrow))))
  (is (not (state/away? "<@test-user>")))

  (is (= (mention (str "<@test-user> is away until " (time/n-days-from-today 3)))
         (str "OK, now I know <@test-user> will be away from "
              (time/today) " to "
              (time/n-days-from-today 2))))

  (is (= (mention "what is <@test-user> schedule?")
         (str "<@test-user> will be away"
              " on " (time/tomorrow)
              ", from " (time/today) " to " (time/n-days-from-today 2))))
  (is (state/away? "<@test-user>"))
  (is (= (mention "clear <@test-user> schedule")
         "OK, I've cleared <@test-user>'s schedule"))

  (is (= (mention (str "<@test-user> will be away from "
                       (time/n-days-ago-today 3) " to "
                       (time/n-days-ago-today 1)))
         (str "OK, now I know <@test-user> will be away from "
              (time/n-days-ago-today 3) " to "
              (time/n-days-ago-today 1))))

  (is (= (mention "what is <@test-user> schedule?")
         "I do not have a schedule for <@test-user>"))
  (is (not (state/away? "<@test-user>")))

  (is (= (mention (str "<@test-user> will be away from "
                       (time/n-days-from-today 2) " to "
                       (time/n-days-from-today 5)))
         (str "OK, now I know <@test-user> will be away from "
              (time/n-days-from-today 2) " to "
              (time/n-days-from-today 5))))

  (is (= (mention "what is <@test-user> schedule?")
         (str "<@test-user> will be away from "
              (time/n-days-from-today 2) " to "
              (time/n-days-from-today 5))))
  (is (not (state/away? "<@test-user>"))))
