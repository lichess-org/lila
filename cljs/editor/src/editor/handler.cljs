(ns org.lichess.editor.handler
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [org.lichess.editor.data :as data]
            [chessground.handler :as cg-handler]
            [chessground.data :as cg-data]
            [chessground.fen :as cg-fen]))

(defn- do-chessground [f] #(update-in % [:chessground] f))

(defn- do-process [k msg]
  (case k
    :set-color        #(assoc % :color msg)
    :set-castle       #(data/set-castle % msg)
    :start            (do-chessground #(chessground.data/with-fen % "start"))
    :clear            (do-chessground #(chessground.data/with-fen % "8/8/8/8/8/8/8/8"))
    (do-chessground   (chessground.handler/process k msg))))

(defn process
  "Return a function that transforms an app data"
  [k msg]
  (fn [app]
    (let [new-app ((do-process k msg) app)]
      (cond-> new-app
        (contains? new-app :chessground) data/with-fen))))

