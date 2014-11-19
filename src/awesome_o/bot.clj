(ns awesome-o.bot
  (:require [awesome-o.parser :as parser]
            [awesome-o.state :as state]
            [awesome-o.time :as time]
            [awesome-o.adam-sandler :as movie]
            [clojure.string :as string]))

(defn thanks? [text]
  (re-matches #"(?i).*(thank|thx|tack).*" text))

(defn meaning-of-life? [text]
  (and (re-matches #"(?i).*(meaning).*" text)
       (re-matches #"(?i).*(life).*" text)))

(defmulti react first)

(defmethod react :help [_]
  (string/join "\n"
               ["You can tell me one of the following:"
                " - I'm a puggle"
                " - person is a puggle"
                " - person is a dev"
                " - who is person?"
                " - location is a location"
                " - person is in location"
                " - where is person?"
                " - person was born on 1980-01-01"
                " - when is person's birthday?"
                " - who is slackmaster?"
                " - person is away today"
                " - person is away from monday to friday"
                " - select the next slackmaster!"
                " - what is the schedule of person?"
                " - clear the schedule of person."
                " - what is the meaning of life?"
                " - generate an Adam Sandler movie idea"]))

(defmethod react :declare-person
  [[_ {:keys [word person]}]]
  (let [name (or word person)]
    (do (state/add-person name)
        (str "OK, nice to meet you @" name "!"))))

(defmethod react :forget-person
  [[_ {:keys [person]}]]
  (do (state/remove-persons-job person)
      (state/remove-birthday person)
      (state/remove-persons-location person)
      (state/reset-periods-away person)
      (state/remove-person person)
      (str "OK, I've forgotten everything about " person)))

(defmethod react :list-team
  [[_ {:keys [job]}]]
  (let [members (state/get-job-persons job)]
    (if (seq members)
      (str "The " job " team: " (string/join ", " members))
      (str "THERE IS NO " (string/upper-case job) ", RUN FOR YOUR LIFE!!"))))

(defmethod react :declare-location
  [[_ {:keys [word]}]]
  (do (state/add-location word)
      (str "OK, now I now that " word " is a location")))

(defmethod react :set-location
  [[_ {:keys [person location]}]]
  (do (state/set-persons-location person location)
      (str "OK, now I know that " person
           " is in " location)))

(defmethod react :get-location
  [[_ {:keys [person]}]]
  (if-let [location (state/get-persons-location person)]
    (str person " is in " location)
    (str "I don't know where " person " is")))

(defmethod react :set-job
  [[_ {:keys [person job]}]]
  (do (state/set-persons-job person job)
      (str "OK, now I know " person
           " is part of the " job " team")))

(defmethod react :get-job
  [[_ {:keys [person]}]]
  (if-let [job (state/get-persons-job person)]
    (str person " is part of the " job " team")
    (str "I don't know which team " person " is part of")))

(defmethod react :who-is
  [[_ {:keys [person]}]]
  (let [job (state/get-persons-job person)
        location (state/get-persons-location person)]
    (str person " is a puggle"
         (when job (str " part of the " job " team"))
         (when location (str " located at the " location " office")))))

(defmethod react :set-birthday
  [[_ {:keys [person date]}]]
  (do (state/set-birthday person date)
      (str "OK, now I know " person
           " is born on " date)))

(defmethod react :get-birthday
  [[_ {:keys [person]}]]
  (if-let [birthday (state/get-birthday person)]
    (str person " was born on " birthday)
    (str "I don't know " person "'s birthday")))

(defmethod react :set-away
  [[_ {:keys [person period]}]]
  (do (state/add-period-away person period)
      (str "OK, now I know " person
           " will be away from " (period :from)
           " to " (period :to)
           (when-not (state/get-slackmaster)
             (str "\n" person " was slackmaster but is away, therefore:\n"
                  (react [:select-next-slackmaster]))))))

(defmethod react :get-schedule
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

(defmethod react :reset-schedule
  [[_ {:keys [person]}]]
  (do (state/reset-periods-away person)
      (str "OK, I've cleared " person "'s schedule")))

(defmethod react :select-next-slackmaster [_]
  (do (state/select-next-slackmaster)
      (react [:get-slackmaster])))

(defmethod react :get-slackmaster [_]
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
     (parser/success? parse-result) (react parse-result)
     (movie/adam-sandler? text) (movie/generate-movie)
     (meaning-of-life? text) "forty-two"
     (thanks? text) (str "you're welcome, @" user-name)
     :else (str user-name ": does not compute:\n"
                "```\n"
                (parser/failure->string parse-result)
                "\n```"))))
