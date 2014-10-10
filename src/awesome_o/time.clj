(ns awesome-o.time
  (:require
   [clj-time.core :as time :refer [days from-now year month day]]
   [clj-time.predicates :refer :all]
   [clj-time.format]))

(defn today []
  (time/today))

(defn next-day [date]
  (time/plus date (days 1)))

(defn previous-day [date]
  (time/minus date (days 1)))

(def weekdays
  ["monday" "tuesday" "wednesday" "thursday" "friday"
   "saturday" "sunday"])

(defn tomorrow []
  (time/plus (today) (days 1)))

(def date-format
  (clj-time.format/formatter "yyyy-MM-dd"))

(defn format-date [date]
  (->> date
       ((juxt year month day))
       (apply time/date-time)
       (clj-time.format/unparse date-format)))

(defn parse-date [date]
  (->> date
       (clj-time.format/parse date-format)
       ((juxt year month day))
       (apply time/local-date)))

(defn relevant-period [{:keys [from to] :as period}]
  (when (time/before? (time/today) (next-day (parse-date to)))
    period))

(defn active-period [{:keys [from to] :as period}]
  (when (time/within? (previous-day (parse-date from))
                      (next-day (parse-date to))
                      (time/today))
    period))

(defn format-period [{:keys [from to]}]
  (str "from " from " to " to))

(defn- next-date [date pred]
  (if (pred date)
    date
    (recur (time/plus date (days 1)) pred)))

(defn- prev-date [date pred]
  (if (pred date)
    date
    (recur (time/minus date (days 1)) pred)))


(defn- weekday-predicate [weekday]
  (resolve (symbol (str "clj-time.predicates/" weekday "?"))))

(defn next-weekday
  ([weekday] (next-weekday (today) weekday))
  ([date weekday]
     (next-date date (weekday-predicate weekday))))

(defn prev-weekday
  ([weekday] (prev-weekday (today) weekday))
  ([date weekday]
     (prev-date date (weekday-predicate weekday))))
