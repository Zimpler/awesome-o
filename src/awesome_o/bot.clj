(ns awesome-o.bot
  (:require [awesome-o.slack :as slack]
            [clojure.string :as string]
            [awesome-o.adam-sandler :as movie]))

(defn announcement [user-name text]
  (slack/say "@EVERYONE: NEW ANNOUNCEMENT FROM " (string/upper-case user-name) ":\n" text))

(defn thanks? [text]
  (re-matches #"(?i).*(thank|thx|tack).*" text))

(defn mention [user-name text]
  (cond (movie/adam-sandler? text) (slack/say (movie/generate-movie))
        (thanks? text) (slack/say "YOU'RE WELCOME, @" (string/upper-case user-name))
        :else (slack/say "@" user-name ": I AM AWESOME-O. "
                         "YOU CAN ONLY ASK ME TO MAKE A MOVIE SCRIPT.")))
