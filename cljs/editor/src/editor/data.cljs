(ns org.lichess.editor.data
  (:require [org.lichess.editor.common :as common]
            [chessground.data :as cg-data]))

(defn make [config]
  {:chessground (chessground.data/make
                  {:fen (:fen config)
                   :orientation "white"
                   :movable {:free? true
                             :color "both"
                             :drop-off "trash"}
                   :premovable {:enabled? false}})})
