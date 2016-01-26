(ns awesome-o.parser
  (:require [clojure.string :as string]
            [clojure.core.match :refer [match]]
            [awesome-o.time :as time
             :refer [today tomorrow weekdays next-weekday
                     prev-weekday parse-date format-date
                     previous-day]]
            [instaparse.core :as insta]))

(defn- ->string-list [data]
  (if (seq data)
    (string/join " | " (map #(str "'" % "'") data))
    "epsilon"))

(defn- dialogue [persons locations jobs]
  (insta/parser
   (str
    "<mention> = (<'awesome-o'> | <'awesomeo'> | <'awesomo'> | <'bot'> | <'test'>) <' '> dialogue
     <dialogue> = help / get-slackmaster / select-next-slackmaster /
                  get-meetingmaster / select-next-meetingmaster /
                  set-job / set-birthday / get-birthday /
                  set-away / set-location / get-location /
                  get-schedule / reset-schedule / declare-person /
                  forget-person / list-team / who-is

     help = <'help'>

     get-slackmaster = who-is-the slackmaster
     select-next-slackmaster = select-the-next slackmaster
     get-meetingmaster = who-is-the meetingmaster
     select-next-meetingmaster = select-the-next meetingmaster

     declare-person = (myself / word) <to-be> <' a '>
                      (<'person'> | <'puggle'>)

     forget-person = <'forget about '> person

     set-job = someone <' is in the '> job <' team'>

     list-team = <'who is part of the '> job <' team'> |
                 <'who is part of '> job

     who-is = <'who is '> person

     set-location = someone <to-be> <' in '> location

     get-location = <'where'> <to-be> <' '> someone

     set-birthday = <'the birthday of '> person <' is '> date |
                    myself <' birthday is '> date |
                    date <' is '> someone <'\\'s'>? <' birthday'> |
                    someone <to-be> <' born on '> date

     get-birthday = <'when is the birthday of '> person |
                    <'when is '> person <'\\'s'>? <' birthday'>

     set-away = someone <to-be> <' away '> period |
                someone <' will be away '> period

     get-schedule = <'what is the schedule of '> someone |
                    <'what is '> someone <'\\'s'>? <' schedule'> |
                    <'when is '> someone <' away'>

     reset-schedule = (<'forget'> | <'clear'> ) <' '> someone <'\\'s'>? <' schedule'> |
                      (<'forget'> | <'clear'> ) <' the schedule of '> person

     to-be = <' is'> | <' am'> | <' was'> | <'\\'m'> | <'\\'s'> | <' will be'> | <'\\'ll be'>

     job = " (->string-list jobs) "

     myself = <'myself'> / <'my'> / <'me'> / <'i'>
     who-is-the = <'who is '> <'the '>?
     select-the-next = <'select '> <'the '>? <'next '>
     everybody = <'everybody'> | <'everyone'>
     person = " (->string-list persons) "
     <someone> = person / myself
     weekday = " (->string-list time/weekdays) "
     <slackmaster> = <'slack'><' '>?<'master'>
     <meetingmaster> = <'meeting'><' '>?<'master'>

     iso-date = #'[0-9]{4}-[0-9]{2}-[0-9]{2}'
     <day> = <'on '>? (weekday | iso-date)
     date = 'today' | 'tomorrow' | day
     period = 'today' | 'tomorrow' | 'this week' | 'this month' |
               'on' <' '> date | 'until' <' '> date |
               'from' <' '> date <' '> 'to' <' '> date

     word = #'[^\\s]+'
     location = " (->string-list locations) "
") :string-ci true))

(defn- normalize-date [data & {:keys [start-date]}]
  (format-date
   (match [data]
     ["today"] (today)
     ["tomorrow"] (tomorrow)
     [[:weekday day]] (next-weekday (or start-date (today)) day)
     [[:prev-weekday day]] (prev-weekday (or start-date (today)) day)
     [[:iso-date date]] (parse-date date))))

(defn- normalize-period [data]
  (match [data]
    [["today"]] {:from (normalize-date "today")
                 :to   (normalize-date "today")}

    [["tomorrow"]] {:from (normalize-date "tomorrow")
                    :to   (normalize-date "tomorrow")}

    [["this week"]] {:from (normalize-date [:prev-weekday "monday"])
                     :to   (normalize-date [:weekday "sunday"])}

    [["on" [:date date]]] (let [norm-date (normalize-date date)]
                           {:from norm-date :to norm-date})

    [["until" [:date date]]] {:from (normalize-date "today")
                              :to (-> date normalize-date
                                      parse-date
                                      previous-day
                                      format-date)}

    [["from" [:date start] "to" [:date end]]]
    (let [from (normalize-date start)
          to   (normalize-date end :start-date (parse-date from))]
      {:from from :to to})))

(defn- normalize-argument [myself data]
  (match [data]
    [[:myself]] [:person myself]
    [[:date date]] [:date (normalize-date date)]
    [[:period & period]] [:period (normalize-period period)]
    [[:job [job]]] [:job job]
    [[key]] [key true]
    [[key value]] [key value]))

(defn- normalize [myself [action & args]]
  [action (->> args
               (mapcat (partial normalize-argument myself))
               (apply hash-map))])

(defn- cleanup [text]
  (-> text
      string/lower-case
      (string/replace #"[,.!?:@]" " ")
      (string/replace "’" "'")
      (string/replace #"[\s]+" " ")
      string/trim))

(defn success? [result]
  (not (insta/failure? result)))

(defn parse [{:keys [myself persons locations jobs]} text]
  (let [result (insta/parse (dialogue persons locations jobs)
                            (cleanup text))]
    (if (insta/failure? result)
      result
      (normalize myself (first result)))))

(defn failure->string [failure]
  (with-out-str (instaparse.failure/print-reason failure)))
