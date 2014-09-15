(ns org.lichess.puzzle.handler
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [org.lichess.puzzle.xhr :as xhr]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn do-process [k msg]
  (case k
    :reload                   #(data/initiate (data/reload % msg))
    :reload-with-progress     #(data/initiate (data/reload-with-progress % msg))
    :play-initial-move        data/play-initial-move
    :play-opponent-next-move  data/play-opponent-next-move
    :user-move                #(data/user-move % msg)
    :set-difficulty           #(xhr/set-difficulty % msg)
    :give-up                  #(xhr/attempt % false)
    :new                      #(xhr/new-puzzle %)
    :retry                    #(xhr/retry-puzzle %)
    :revert                   data/revert
    :vote                     #(xhr/vote % msg)
    :set-votes                #(data/set-votes % msg)
    :first                    #(data/jump % (fn [_] 0))
    :prev                     #(data/jump % dec)
    :next                     #(data/jump % inc)
    :last                     #(data/jump % (fn [_] 999))
    (do-chessground           (chessground.handler/process k msg))))

(defn process
  "Return a function that transforms an app data"
  [k msg]
  (fn [app]
    (let [new-app ((do-process k msg) app)]
      (if (contains? new-app :puzzle) new-app app))))
