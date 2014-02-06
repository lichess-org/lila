(ns lichess.puzzle.main
  (:require [lichess.puzzle.core :as core]
            [lichess.puzzle.play :as play]
            [lichess.puzzle.replay :as replay]
            [jayq.core :as jq :refer [$]]))

(defn playing? [] (jq/has-class core/$puzzle "playing"))

(if (playing?) (play/run!) (replay/run!))
