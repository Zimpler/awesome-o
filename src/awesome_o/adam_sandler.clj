(ns awesome-o.adam-sandler
  (:require [clojure.string :as string]))

(defn adam-sandler? [text]
  (and (re-matches #"(?i).*(movie|film).*" text)
       (re-matches #"(?i).*(idea|plot|script).*" text)))

(def intros
  ["hum... let's see... how about:"
   "okay, how about this:"
   "hum... how about this:"
   "hum... okay... hum... how about:"])

(def subjects
  ["his girlfriend"
   "his boss"
   "his cousin"
   "his friend"
   "his dog"])

(def verbs
  ["falls in loves with"
   "will inherit from"
   "has to find"
   "lost"
   "is afraid of"
   "is looking for"
   "arrives on a deserted island with"])

(def transitions
  ["but in order to succeed, he has to fight"
   "but then realises that actually it's"
   "and then falls in love with"])

(def second-subjects
  ["a cococut"
   "a golden retriewer"
   "a carrot"
   "rob schneider"])

(defn generate-movie []
  (string/join "" [(rand-nth intros)
                   " Adam Sandler "
                   (rand-nth verbs)
                   " "
                   (rand-nth subjects)
                   ", "
                   (rand-nth transitions)
                   " "
                   (rand-nth second-subjects)
                   ", or something..?"]))
