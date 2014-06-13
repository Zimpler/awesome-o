(ns awesome-o.bot
  (:require [awesome-o.slack :as slack]
            [awesome-o.adam-sandler :as movie]))

(defn announcement [user-name text]
  (slack/say "@everyone: new announcement from " user-name ":\n" text))

(defn mention [user-name text]
  (cond (movie/adam-sandler? text) (slack/say (movie/generate-movie))
        :else (slack/say "@" user-name ": I AM AWESOME-O. "
                         "YOU CAN ONLY ASK ME TO MAKE A MOVIE SCRIPT.")))
