(ns awesome-o.state
  (:require [taoensso.carmine :as car :refer (wcar)]
            [awesome-o.time :as time]
            [taoensso.carmine.locks :as locks]
            [environ.core :refer [env]]))

; All methods that use redis - make sure to properly mock them in bot-test:12

(defn- redis-config []
  {:pool {}
   :spec {:uri (env :redis-url "redis://localhost:6379")}})

(defmacro ^:private wcar* [& body] `(car/wcar (redis-config) ~@body))

(defn flushdb []
  (wcar* (car/flushdb)))

(defn- ismember? [key value]
  (pos? (wcar* (car/sismember key value))))


(defn reset-state []
  (wcar* (car/set "state"
                  {:persons {}})))

(defn get-state []
  (wcar* (car/get "state")))


(defn- update-state [fun]
  (let [state (get-state)]
    (wcar* (car/set "state" (fun state)))))


(defn reset-daily-announcement []
  (wcar* (car/set (str "daily-announcement-"
                       (time/format-date (time/today)))
                  nil)))

(defn acquire-daily-announcement []
  (not (wcar* (car/getset (str "daily-announcement-"
                               (time/format-date (time/today)))
                          true))))

; 👇 Here be functions that don't directly use redis 👇

(declare away?)

(defn- get-in-state [ks]
  (get-in (get-state) ks))

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
  (get-in-state [:persons name key]))

(defn- get-persons-key [key]
  (->> (get-state)
       :persons
       (sort-by (comp :position second))
       (map (fn [[name person]] [name (person key)]))
       (into (array-map))))

(defn locations-in-state []
  (->> (get-state)
       :persons
       (mapv (fn [[_ data]] (:location data)))
       (filter (comp not nil?))
       (distinct)))

(defn remove-person [name]
  (update-in-state [:persons] dissoc name))

(defn get-names []
  (-> (get-state) :persons keys))

(defn set-birthday [name date]
  (set-person-key name :birthday date))

(defn get-birthday [name]
  (get-person-key name :birthday))

(defn- get-birthdays []
  (filter (fn [[_ date]] (some? date)) (get-persons-key :birthday)))

(defn persons-born-today []
  (->> (get-birthdays)
       (filter
        (fn [[_ date]]
          (time/same-day-and-month? (time/parse-date date)
                                    (time/today))))
       (map first)))

(defn set-persons-location [name location]
  (set-person-key name :location location))

(defn get-persons-location [name]
  (get-person-key name :location))

(def valid-locations ["stockholm" "göteborg" "malta" "remote"])

(def jobs ["dev" "sales" "biz" "bizdev" "design"])

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

(defn draw-people-from-job [job & {:keys [number]}]
  (->> (get-job-persons job)
       (remove away?)
       (shuffle)
       (take number)))

(defn random-person []
  (->> (get-state)
       :persons
       (remove (fn [[person _]] (away? person)))
       (mapv first)
       rand-nth))

(defn random-person-from-location [target-location]
  (->> (get-persons-key :location)
       (filter (fn [[_ location]] (= target-location location)))
       (remove (fn [[person _]] (away? person)))
       (mapv first)
       rand-nth))

(defn random-person-from-other-location [name]
  (let [target-location (get-persons-location name)]
    (->> (get-persons-key :location)
         (filter (fn [[_ location]] (or (= location "remote")
                                        (not= location target-location))))
         (remove (fn [[person _]] (or (away? person)
                                      (= person name))))
         (mapv first)
         rand-nth)))

(defn three-random-people-from-different-locations []
  (for [location (take 3 (shuffle (locations-in-state)))]
    (random-person-from-location location)))

(defn random-person-with-job [job]
  (->> (get-job-persons job)
       (remove away?)
       (into [])
       rand-nth))
