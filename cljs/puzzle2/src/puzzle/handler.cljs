(ns org.lichess.puzzle.handler
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn process [k msg ctrl]
  "Return a function that transforms an app data"
  (case k
    :reload                   #(data/make (js->clj msg :keywordize-keys true) ctrl)
    :play-initial-move        data/play-initial-move
    :play-opponent-next-move  data/play-opponent-next-move
    :user-move                #(data/user-move % msg ctrl)
    :give-up                  data/give-up
    (do-chessground           (chessground.handler/process k msg))))
