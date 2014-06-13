(ns awesome-o.adam-sandler
  (:require [clojure.string :as string]))

(defn adam-sandler? [text]
  (and (re-matches #"(?i).*(movie|film).*" text)
       (re-matches #"(?i).*(idea|plot|script).*" text)))

(def intros
  ["HUM... LET'S SEE... HOW ABOUT:"
   "OKAY, HOW ABOUT THIS:"
   "HUM... HOW ABOUT THIS:"
   "HUM... OKAY... HUM... HOW ABOUT:"])

(def subjects
  ["HIS GIRLFRIEND"
   "HIS BOSS"
   "HIS COUSIN"
   "HIS FRIEND"
   "HIS DOG"])

(def verbs
  ["FALLS IN LOVES WITH"
   "WILL INHERIT FROM"
   "HAS TO FIND"
   "LOST"
   "IS AFRAID OF"
   "IS LOOKING FOR"
   "ARRIVES ON A DESERTED ISLAND WITH"])

(def transitions
  ["BUT IN ORDER TO SUCCEED, HE HAS TO FIGHT"
   "BUT THEN REALISES THAT ACTUALLY IT'S"
   "AND THEN FALLS IN LOVE WITH"])

(def second-subjects
  ["A COCOCUT"
   "A GOLDEN RETRIEWER"
   "A CARROT"
   "ROB SCHNEIDER"])

(defn generate-movie []
  (string/join "" [(rand-nth intros)
                   " ADAM SANDLER "
                   (rand-nth verbs)
                   " "
                   (rand-nth subjects)
                   ", "
                   (rand-nth transitions)
                   " "
                   (rand-nth second-subjects)
                   ", OR SOMETHING..?"]))
