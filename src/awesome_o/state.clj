(ns awesome-o.state
  (:require [taoensso.carmine :as car :refer (wcar)]
            [awesome-o.time :as time]
            [taoensso.carmine.locks :as locks]
            [environ.core :refer [env]]))

(defn redis-config []
  {:pool {}
   :spec {:uri (env :redistogo-url "redis:://localhost:6379")}})

(defmacro wcar* [& body] `(car/wcar (redis-config) ~@body))

(defn flushdb []
  (wcar* (car/flushdb)))

(defn- ismember? [key value]
  (pos? (wcar* (car/sismember key value))))

(defn reset-state []
  (wcar* (car/set "state"
                  {:persons {}
                   :locations []
                   :slackmasters-index 0
                   :dev-meeting-index 0})))

(defn get-state []
  (wcar* (car/get "state")))

(defn update-state [fun]
  (let [state (get-state)]
    (wcar* (car/set "state" (fun state)))))

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
                    :position (+ last-position 1)}))))))

(defn set-person-key [name key value]
  (do (add-person name)
      (update-state
       (fn [state]
         (update-in state [:persons name] assoc key value)))))

(defn get-person-key [name key]
  (get-in (get-state) [:persons name key]))

(defn get-persons-key [key]
  (->> (get-state)
       :persons
       (sort-by (comp :position second))
       (map (fn [[name person]] [name (person key)]))
       (into (array-map))))

(defn remove-person [name]
  (update-state
   (fn [state] (update-in state [:persons] dissoc name))))

(defn get-names []
  (-> (get-state) :persons keys))

(defn set-birthday [name date]
  (set-person-key name :birthday date))

(defn get-birthday [name]
  (get-person-key name :birthday))

(defn get-birthdays []
  (get-persons-key :birthday))

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


(defn add-location [location]
  (update-state
   (fn [state]
     (update-in state [:locations] conj location))))

(defn remove-location [location]
  (update-state
   (fn [state]
     (update-in state [:locations]
                (fn [locations]
                  (vec (remove #{location} locations)))))))

(defn get-locations []
  (get (get-state) :locations))

(defn get-available-people-in-location [target-location]
  (for [[person location] (get-persons-locations)
        :when (= location target-location)
        :when (not (is-away person))]
    person))

(def jobs ["dev" "sales" "biz" "bizdev" "ux"])

(defn remove-persons-job [name]
  (set-person-key name :team nil))

(defn set-persons-job [name new-job]
  (set-person-key name :team new-job))

(defn get-persons-job [name]
  (get-person-key name :team))

(defn get-job-persons [job]
  (->> (get-persons-key :team)
       (filter (fn [[name j]] (= j job)))
       (map first)))


(defn add-period-away [name period]
  (update-state
   (fn [state]
     (update-in state [:persons name :away] conj period))))

(defn get-periods-away [name]
  (get-person-key name :away))

(defn reset-periods-away [name]
  (set-person-key name :away []))

(defn is-away [person]
  (some time/active-period (get-periods-away person)))

(defn available-devs []
  (remove is-away (get-job-persons "dev")))

(defn reset-slackmaster []
  (update-state
   (fn [state] (assoc state :slackmasters-index 0))))

(defn get-slackmaster-index []
  (get (get-state) :slackmasters-index))

(defn get-slackmaster []
  (let [slackmaster
        (nth (cycle (get-job-persons "dev"))
             (get-slackmaster-index))]
    (when-not (is-away slackmaster)
      slackmaster)))

(defn select-next-slackmaster []
  (do
    (update-state
     (fn [state]
       (update-in state [:slackmasters-index] inc)))
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
