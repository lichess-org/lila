(ns org.lichess.puzzle.data
  (:require [chessground.common :refer [pp]]
            [chessground.data :as cg-data]
            [chessground.common :as cg-common]
            [chessground.api :as cg-api]))

(defn make [config]
  {:color (:color config)
   :chessground (chessground.api/main
                  {:fen (:fen config)
                   :orientation "white"
                   :movable {:free true
                             :color "both"
                             :dropOff "trash"}
                   :premovable {:enabled false}})})
