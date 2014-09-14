(ns org.lichess.puzzle.handler
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn process [k msg]
  "Return a function that transforms an app data"
  (case k
    :play-initial-move  data/play-initial-move
    (do-chessground     (chessground.handler/process k msg))))
