(ns org.lichess.puzzle.chess
  (:require [chessground.common :refer [pp map-values]]))

(defn make [fen]
  (let [ch (js/Chess.)]
    (.load ch fen)
    ch))

(defn move [ch [orig dest prom]]
  (.move ch #js {:from orig :to dest :promotion prom}))

(defn parse-move [m]
  (when m [(aget m "from") (aget m "to")]))

(defn dests [ch]
  (let [moves (.moves ch #js {:verbose true})
        grouped (group-by first (map parse-move moves))]
    (into {} (map-values #(map second %) grouped))))

(defn get-last-move [ch]
  (let [hist (.history ch #js {:verbose true})]
    (parse-move (aget hist (-> hist .-length dec)))))

