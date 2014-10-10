(ns awesome-o.bot
  (:require [awesome-o.slack :as slack]
            [clojure.string :as string]
            [awesome-o.parser :as parser]
            [awesome-o.state :as state]
            [awesome-o.time :as time]
            [awesome-o.adam-sandler :as movie]))

(defn announcement [user-name text]
  (slack/say "@everyone: NEW ANNOUNCEMENT FROM " (string/upper-case user-name) ":\n" text))

(defn thanks? [text]
  (re-matches #"(?i).*(thank|thx|tack).*" text))

(defn meaning-of-life? [text]
  (and (re-matches #"(?i).*(meaning).*" text)
       (re-matches #"(?i).*(life).*" text)))

(defmulti process-parse-result first)

(defmethod process-parse-result :declare-person
  [[_ {:keys [word]}]]
  (do (state/add-person word)
      (str "OK, nice to meet you @" word "!")))

(defmethod process-parse-result :declare-location
  [[_ {:keys [word]}]]
  (do (state/add-location word)
      (str "OK, now I now that " word " is a location")))

(defmethod process-parse-result :get-slackmaster [_]
  "who is slackmaster")

(defmethod process-parse-result :set-location
  [[_ {:keys [person location]}]]
  (do (state/set-persons-location person location)
      (str "OK, now I know that " person
           " is in " location)))

(defmethod process-parse-result :get-location
  [[_ {:keys [person]}]]
  (if-let [location (state/get-persons-location person)]
    (str person " is in " location)
    (str "I don't know where " person " is")))

(defmethod process-parse-result :set-job
  [[_ {:keys [person job]}]]
  (do (state/set-persons-job person job)
      (str "OK, now I know " person
           " is part of the " job " team")))

(defmethod process-parse-result :get-job
  [[_ {:keys [person]}]]
  (if-let [job (state/get-persons-job person)]
    (str person " is part of the " job " team")
    (str "I don't know which team " person " is part of")))

(defmethod process-parse-result :who-is
  [[_ {:keys [person]}]]
  (let [job (state/get-persons-job person)
        location (state/get-persons-location person)]
    (str person " is a puggle"
         (when job (str " part of the " job " team"))
         (when location (str " located at the " location " office")))))

(defmethod process-parse-result :set-birthday
  [[_ {:keys [person date]}]]
  (do (state/set-birthday person date)
      (str "OK, now I know " person
           " is born on " date)))

(defmethod process-parse-result :get-birthday
  [[_ {:keys [person]}]]
  (if-let [birthday (state/get-birthday person)]
    (str person " was born on " birthday)
    (str "I don't know " person "'s birthday")))

(defmethod process-parse-result :set-away
  [[_ {:keys [person period]}]]
  (do (state/add-period-away person period)
      (str "OK, now I know " person
           " will be away from " (period :from)
           " to " (period :to)
           (when-not (state/get-slackmaster)
             (str "\n" person " was slackmaster but is away, therefore:\n"
                  (process-parse-result [:select-next-slackmaster]))))))

(defmethod process-parse-result :get-schedule
  [[_ {:keys [person]}]]
  (let [away-periods (->> person
                          state/get-periods-away
                          (filter time/relevant-period))]
    (if (seq away-periods)
      (str person " will be away "
           (->> away-periods
                (map time/format-period)
                (string/join ", ")))
      (str "I do not have a schedule for " person))))

(defmethod process-parse-result :reset-schedule
  [[_ {:keys [person]}]]
  (do (state/reset-periods-away person)
      (str "OK, I've cleared " person "'s schedule")))

(defmethod process-parse-result :select-next-slackmaster [_]
  (do (state/select-next-slackmaster)
      (process-parse-result [:get-slackmaster])))

(defmethod process-parse-result :get-slackmaster [_]
  (if-let [slackmaster (state/get-slackmaster)]
    (str "@" slackmaster " is today's slackmaster")
    (str "THERE IS NO DEV! OMG RUN FOR YOUR LIFE!!")))

(defn reply [user-name text]
  (let [persons (state/get-persons)
        locations (state/get-locations)
        parse-result (parser/parse {:myself user-name
                                    :persons persons
                                    :locations locations}
                                   text)]
    (cond
     (parser/success? parse-result) (process-parse-result parse-result)
     (movie/adam-sandler? text) (movie/generate-movie)
     (meaning-of-life? text) "forty-two"
     (thanks? text) (str "you're welcome, @" user-name)
     :else (str user-name ": does not compute:"
                (parser/failure->string parse-result)))))

(defn mention [user-name text]
  (slack/say (reply user-name text)))
