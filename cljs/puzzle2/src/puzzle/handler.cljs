(ns org.lichess.puzzle.handler
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [org.lichess.puzzle.xhr :as xhr]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn do-process [k msg ctrl]
  (case k
    :set-difficulty           #(xhr/set-difficulty % msg ctrl)
    :reload                   #(data/reload msg ctrl)
    :reload-with-progress     #(data/reload-with-progress % msg ctrl)
    :play-initial-move        data/play-initial-move
    :play-opponent-next-move  data/play-opponent-next-move
    :user-move                #(data/user-move % msg ctrl)
    :give-up                  #(xhr/attempt % false ctrl)
    (do-chessground           (chessground.handler/process k msg))))

(defn process
  "Return a function that transforms an app data"
  [k msg ctrl]
  (fn [app]
    (let [new-app ((do-process k msg ctrl) app)]
      (if (contains? new-app :puzzle) new-app app))))
