(ns org.lichess.puzzle.handler
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn- do-process [k msg]
  (case k
    (do-chessground   (chessground.handler/process k msg))))

(defn process
  "Return a function that transforms an app data"
  [k msg]
  (fn [app]
    (do-process k msg)))
