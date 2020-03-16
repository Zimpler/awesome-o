(ns awesome-o.http
  (:require
   [cheshire.core :as json]
   [org.httpkit.client :as http]
   [org.httpkit.sni-client :as sni-client]))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))

(def ^:private url
  "https://zimpler.slack.com/services/hooks/incoming-webhook")

(defn post
  [token payload]
  (let [r @(http/post
            url
            {:query-params {:token token}
             :form-params {:payload (json/generate-string payload)}})]
    (println r)
    r))
