(ns org.lichess.puzzle.chess
  (:require [chessground.common :refer [pp map-values]]))

(defn make [fen]
  (let [ch (js/Chess.)]
    (.load ch fen)
    ch))

(defn move [ch [orig dest prom]]
  (.move ch #js {:from orig :to dest :promotion prom}))

(defn dests [ch]
  (let [moves (.moves ch #js {:verbose true})
        parse (fn [m] [(aget m "from") (aget m "to")])
        grouped (group-by first (map parse moves))]
    (into {} (map-values #(map second %) grouped))))
