(ns awesome-o.state
  (:require [taoensso.carmine :as car :refer (wcar)]
            [awesome-o.time :as time]
            [taoensso.carmine.locks :as locks]
            [environ.core :refer [env]]))

(defn redis-config []
  {:pool {}
   :spec {:uri (env :redistogo-url "redis://localhost:6379")}})

(defmacro wcar* [& body] `(car/wcar (redis-config) ~@body))

(defn flushdb []
  (wcar* (car/flushdb)))

(defn- ismember? [key value]
  (pos? (wcar* (car/sismember key value))))

(declare away?)

(defn reset-state []
  (wcar* (car/set "state"
                  {:persons {}
                   :task-assignments {:slack-master nil}})))

(defn- get-state []
  (wcar* (car/get "state")))

(defn- update-state [fun]
  (let [state (get-state)]
    (wcar* (car/set "state" (fun state)))))

(defn- update-in-state [ks f & args]
  (update-state (fn [state] (apply update-in state ks f args))))

(defn add-person [name]
  (update-state
   (fn [state]
     (let [last-position (->> state :persons vals (map :position) (apply max 0))]
       (if (get-in state [:persons name])
         state
         (assoc-in state [:persons name]
                   {:birthday nil
                    :location nil
                    :team nil
                    :away []
                    :position (inc last-position)}))))))

(defn- set-person-key [name key value]
  (do (add-person name)
  (update-in-state [:persons name] assoc key value)))

(defn- get-person-key [name key]
  (get-in (get-state) [:persons name key]))

(defn- get-persons-key [key]
  (->> (get-state)
       :persons
       (sort-by (comp :position second))
       (map (fn [[name person]] [name (person key)]))
       (into (array-map))))

(defn remove-person [name]
  (update-in-state [:persons] dissoc name))

(defn get-names []
  (-> (get-state) :persons keys))

(defn set-birthday [name date]
  (set-person-key name :birthday date))

(defn get-birthday [name]
  (get-person-key name :birthday))

(defn get-birthdays []
  (filter (fn [[_ date]] (some? date)) (get-persons-key :birthday)))

(defn remove-birthday [name]
  (set-person-key name :birthday nil))

(defn persons-born-today []
  (->> (get-birthdays)
       (filter
        (fn [[_ date]]
          (time/same-day-and-month? (time/parse-date date)
                                    (time/today))))
       (map first)))

(defn set-persons-location [name location]
  (set-person-key name :location location))

(defn remove-persons-location [person]
  (set-person-key name :location nil))

(defn get-persons-location [name]
  (get-person-key name :location))

(defn get-persons-locations []
  (get-persons-key :location))

(defn get-available-people-in-location [target-location]
  (for [[person location] (get-persons-locations)
        :when (= location target-location)
        :when (not (away? person))]
    person))

(def locations ["stockholm" "göteborg"])

(def jobs ["dev" "sales" "biz" "bizdev" "design"])

(defn remove-persons-job [name]
  (set-person-key name :team nil))

(defn set-persons-job [name new-job]
  (set-person-key name :team new-job))

(defn get-persons-job [name]
  (get-person-key name :team))

(defn get-job-persons [job]
  (->> (get-persons-key :team)
       (filter (fn [[name j]] (= j job)))
       (map first)
       (apply vector)))

(defn add-period-away [name period]
  (update-in-state [:persons name :away] conj period))

(defn get-periods-away [name]
  (get-person-key name :away))

(defn reset-periods-away [name]
  (set-person-key name :away []))

(defn away? [person]
  (some time/active-period (get-periods-away person)))

(defn available-devs []
  (remove away? (get-job-persons "dev")))

(defn select-next-slackmaster []
  (update-in-state [:task-assignments]
                   assoc :slackmaster (rand-nth (available-devs))))

(defn get-slackmaster []
  (or (get-in (get-state) [:task-assignments :slackmaster])
      (do (select-next-slackmaster)
          (get-in (get-state) [:task-assignments :slackmaster]))))

(defn reset-daily-announcement []
  (wcar* (car/set (str "daily-announcement-"
                       (time/format-date (time/today)))
                  nil)))

(defn acquire-daily-announcement []
  (not (wcar* (car/getset (str "daily-announcement-"
                               (time/format-date (time/today)))
                          true))))
