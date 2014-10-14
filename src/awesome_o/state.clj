(ns awesome-o.state
  (:require [taoensso.carmine :as car :refer (wcar)]
            [awesome-o.time :as time]
            [environ.core :refer [env]]))

(defn redis-config []
  {:pool {}
   :spec {:uri (env :redistogo-url "redis:://localhost:6379")}})

(defmacro wcar* [& body] `(car/wcar (redis-config) ~@body))

(defn flushdb []
  (wcar* (car/flushdb)))

(defn- ismember? [key value]
  (pos? (wcar* (car/sismember key value))))

(defn add-person [name]
  (wcar* (car/sadd "persons" name)))

(defn remove-person [name]
  (wcar* (car/srem "persons" name)))

(defn get-persons []
  (wcar* (car/smembers "persons")))


(defn set-birthday [name date]
  (wcar* (car/hset "birthdays" name date)))

(defn get-birthday [person]
  (wcar* (car/hget "birthdays" person)))

(defn get-birthdays []
  (apply hash-map
         (wcar* (car/hgetall "birthdays"))))

(defn persons-born-today []
  (->> (get-birthdays)
       (filter
        (fn [[_ date]]
          (time/same-day-and-month? (time/parse-date date)
                                    (time/today))))
       (map first)))

(defn set-persons-location [name location]
  (wcar* (car/hset "persons-location" name location)))

(defn get-persons-location [name]
  (wcar* (car/hget "persons-location" name)))

(defn get-persons-locations []
  (apply hash-map
         (wcar* (car/hgetall "persons-location"))))

(defn add-location [location]
  (wcar* (car/sadd "locations" location)))

(defn remove-location [location]
  (wcar* (car/srem "locations" location)))

(defn get-locations []
  (wcar* (car/smembers "locations")))


(def jobs ["dev" "sales" "biz" "bizdev" "ux"])

(defn- job-key [job-name]
  (str "persons-job-" job-name))

(def ^:private job-keys (mapv job-key jobs))

(defn set-persons-job [name new-job]
  (wcar*
   (doseq [job-key job-keys] (car/srem job-key name))
   (car/sadd (job-key new-job) name)))

(defn get-persons-job [name]
  (first (filter #(ismember? (job-key %) name)
                 jobs)))

(defn get-job-persons [job]
  (wcar* (car/smembers (job-key job))))


(defn add-period-away [person period]
  (wcar* (car/lpush (str "period-away-" person)
                    period)))

(defn get-periods-away [person]
  (wcar* (car/lrange (str "period-away-" person) 0 100)))

(defn is-away [person]
  (some time/active-period (get-periods-away person)))

(defn reset-periods-away [person]
  (wcar* (car/del (str "period-away-" person))))


(defn available-devs []
  (remove is-away (get-job-persons "dev")))

(defn reset-slackmaster []
  (wcar* (car/set "slackmaster-index" "0")))

(defn get-slackmaster []
  (let [slackmaster
        (nth (cycle (get-job-persons "dev"))
             (read-string
              (or (wcar* (car/get "slackmaster-index")) "0")))]
    (when-not (is-away slackmaster)
      slackmaster)))

(defn select-next-slackmaster []
  (do (wcar* (car/incr "slackmaster-index"))
      (if-let [dev (get-slackmaster)]
        dev
        (when (seq (available-devs))
          (recur)))))

(defn reset-daily-announcement []
  (wcar* (car/set (str "daily-announcement-"
                       (time/format-date (time/today)))
                  nil)))

(defn acquire-daily-announcement []
  (not (wcar* (car/getset (str "daily-announcement-"
                               (time/format-date (time/today)))
                          true))))
