(ns awesome-o.web
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [awesome-o.slack :as slack]
            [uswitch.lambada.core :refer [deflambdafn]]
            [ring.util.codec :refer [form-decode]]
            [awesome-o.state :as state]
            [environ.core :refer [env]]))

(defn decode-body [event]
  (form-decode (get event "body")))

(defn handle-announcements
  [event]
  (let [body (decode-body event)]
    (slack/announcement (str "<@" (get body "user_id") ">")
                        (get body "text")))
  (println body)
  {:statusCode 200 :body ""})

(deflambdafn awesomeo.announcements
  [in out ctx]
  (let [event (json/parse-stream (io/reader in))
        res (handle-announcements event)]
    (with-open [w (io/writer out)]
      (json/generate-stream res w))))

(defn handle-mention
  [event]
  (let [body (decode-body event)
        slack-response (slack/mention (str "<@" (get body "user_id") ">")
                                      (get body "text"))]
    (println body)
    {:statusCode 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/generate-string slack-response)}))

(deflambdafn awesomeo.mention
  [in out ctx]
  (let [event (json/parse-stream (io/reader in))
        res (handle-mention event)]
    (with-open [w (io/writer out)]
      (json/generate-stream res w))))

(defn handle-scheduled
  [event]
  (slack/ping)
  {:statusCode 200 :body ""})

(deflambdafn awesomeo.scheduled
  [in out ctx]
  (let [event (json/parse-stream (io/reader in))
        res (handle-scheduled event)]
    (with-open [w (io/writer out)]
      (json/generate-stream res w))))
