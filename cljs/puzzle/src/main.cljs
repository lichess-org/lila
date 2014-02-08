(ns lichess.puzzle.main
  (:require [lichess.puzzle.core :as core]
            [lichess.puzzle.play :as play]
            [lichess.puzzle.view :as view]
            [jayq.core :as jq :refer [$]]))

(defn playing? [] (jq/has-class ($ :#puzzle) "playing"))

(if (playing?) (play/run!) (view/run! []))
