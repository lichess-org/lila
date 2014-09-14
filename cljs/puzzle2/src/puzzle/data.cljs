(ns org.lichess.puzzle.data
  (:require [chessground.common :refer [pp opposite-color]]
            [chessground.data :as cg-data]
            [chessground.common :as cg-common]
            [chessground.api :as cg-api]))

(defn parse-move
  "e2e4 -> [e2 e4]
   e7e8n -> [e7 e8 n]"
  [move]
  (when move
    (let [[a, b, c, d, p] (seq move)] [(str a b) (str c d) p])))

(defn- apply-move [state [orig dest prom]]
  (.move (:chess state) #js {:from orig :to dest :promotion prom}))

(defn- set-dests [state]
  (let [ch (:chess state)
        dests (into {} (filter second (map (fn [orig dests] (.-SQUARES ch)
  (update-in [:chessground] #(chessground.data/set-movable-dests
      function chessToDests(chess) {
        var dests = {};
        chess.SQUARES.forEach(function(s) {
          var ms = chess.moves({square: s});
          if (ms.length) dests[s] = ms.map(function(m) { return m.substr(-2); });
        });
        return dests;

(defn play-opponent-move [state move]
    (apply-move state move)
        (-> state
            (update-in [:chessground] #(chessground.data/api-move-piece % move))
            (dissoc :initial-move)))
    state))

(defn play-initial-move [state]
  (if-let [move (parse-move (:initial-move state))]
    (do (apply-move state move)
        (-> state
            (update-in [:chessground] #(chessground.data/api-move-piece % move))
            (dissoc :initial-move)))
    state))

(defn on-move [orig dest ctrl]
  (pp [orig dest]))

(defn make [config ctrl]
  {:mode (:mode config) ; view | play | try
   :initial-move (:initialMove config)
   :lines (:lines config)
   :post-url (:postUrl config)
   :chess (let [ch (js/Chess.)]
            (.load ch (:fen config))
            ch)
   :chessground (chessground.api/main
                  {:fen (:fen config)
                   :orientation (:color config)
                   :turnColor (opposite-color (:color config))
                   :movable {:free false
                             :color (:color config)
                             :events {:after #(on-move %1 %2 ctrl)}}
                   :animation {:enabled true
                               :duration 500}
                   :premovable {:enabled false}})})
