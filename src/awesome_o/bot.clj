(ns awesome-o.bot
  (:require [awesome-o.slack :as slack]))

(defn announcement [user-name text]
  (slack/say "@everyone: new announcement from " user-name ":\n" text))
