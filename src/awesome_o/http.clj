(ns awesome-o.http
  (:require
   [cheshire.core :as json]
   [org.httpkit.client :as http]))

(def ^:private url
  "https://zimpler.slack.com/services/hooks/incoming-webhook")

(defn post
  [token payload]
  @(http/post
    url
    {:query-params {:token token}
     :form-params {:payload (json/generate-string payload)}}))
