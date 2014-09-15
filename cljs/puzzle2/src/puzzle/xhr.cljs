(ns org.lichess.puzzle.xhr
  (:require [jayq.core :as jq]))

(defn set-difficulty [{:keys [ctrl router]} id]
  (jq/xhr [:post (router :Puzzle.difficulty)]
          {:difficulty id}
          #(ctrl :reload %)))

(defn attempt [{:keys [puzzle ctrl router started-at]} win]
  (jq/xhr [:post (router :Puzzle.attempt (:id puzzle))]
          {:win (if win 1 0)
           :time (- (.getTime (js/Date.)) (.getTime started-at))}
          #(ctrl :reload-with-progress %)))

(defn new-puzzle [{:keys [ctrl router]}]
  (jq/xhr [:get (router :Puzzle.newPuzzle)]
          {}
          #(ctrl :reload %)))

(defn retry-puzzle [{:keys [puzzle ctrl router]}]
  (jq/xhr [:get (router :Puzzle.load (:id puzzle))]
          {}
          #(ctrl :reload %)))

(defn vote [{:keys [puzzle ctrl router]} v]
  (jq/xhr [:post (router :Puzzle.vote (:id puzzle))]
          {:vote v}
          #(ctrl :set-votes %)))
