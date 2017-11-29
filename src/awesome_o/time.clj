(ns awesome-o.time
  (:require
   [clj-time.core :as time :refer [days year month day]]
   [clj-time.predicates :refer :all]
   [clj-time.format]))

(def stockholm-tz
  (time/time-zone-for-id "Europe/Stockholm"))

(defn now []
  (time/to-time-zone (time/now) stockholm-tz))

(defn today []
  (->> (now)
      ((juxt year month day))
      (apply time/local-date)))

(defn today-at
  ([hour] (today-at hour 0))
  ([hour min] (today-at hour min 0))
  ([hour min sec]
   (time/from-time-zone (time/today-at hour min sec)
                        stockholm-tz)))

(defn working-hour? []
  (and (not (weekend? (now)))
       (time/within? (today-at 8) (today-at 18) (now))))

(defn monday-today? []
  (monday? (today)))

(defn tuesday-today? []
  (tuesday? (today)))

(defn wednesday-today? []
  (wednesday? (today)))

(defn thursday-today? []
  (thursday? (today)))

(defn friday-today? []
  (friday? (today)))

(defn n-days-from-today [n]
  (time/plus (time/today) (days n)))

(defn n-days-ago-today [n]
  (time/minus (time/today) (days n)))

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

(def day-and-month (juxt day month))

(defn same-day-and-month? [date-a date-b]
  (= (day-and-month date-a)
     (day-and-month date-b)))

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
  (when (time/within? (parse-date from)
                      (parse-date to)
                      (time/today))
    period))

(defn format-period [{:keys [from to]}]
  (if (= from to)
    (str "on " from)
    (str "from " from " to " to)))

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
