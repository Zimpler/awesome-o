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
                " - person is in the dev team"
                " - who is person?"
                " - person is in location"
                " - where is person?"
                " - person was born on 1980-01-01"
                " - when is person's birthday?"
                " - person is away today"
                " - person is away from monday to friday"
                " - what is the schedule of person?"
                " - clear the schedule of person."
                " - what is the meaning of life?"
                " - generate an Adam Sandler movie idea"]))

(defmethod react :declare-person
  [[_ {:keys [word person]}]]
  (let [name (or word person)]
    (state/add-person name)
    (str "OK, nice to meet you @" name "!")))

(defmethod react :forget-person
  [[_ {:keys [person]}]]
  (do (state/remove-person person)
      (str "OK, I've forgotten everything about " person)))

(defmethod react :list-team
  [[_ {:keys [job]}]]
  (let [members (state/get-job-persons job)]
    (if (seq members)
      (str "The " job " team: " (string/join ", " members))
      (str "THERE IS NO " (string/upper-case job) ", RUN FOR YOUR LIFE!!"))))

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
           " to " (period :to))))

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

(defn reply [user-name text]
  (let [persons (state/get-names)
        locations state/locations
        jobs      state/jobs
        parse-result (parser/parse {:myself user-name
                                    :persons persons
                                    :locations locations
                                    :jobs jobs}
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
