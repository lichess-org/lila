(ns org.lichess.puzzle.data
  (:require [chessground.common :refer [pp opposite-color]]
            [chessground.data :as cg-data]
            [chessground.common :as cg-common]
            [chessground.api :as cg-api]
            [org.lichess.puzzle.chess :as chess]))

(defn str->move
  "e2e4 -> [e2 e4]
   e7e8n -> [e7 e8 n]"
  [move]
  (when move [(str (aget move 0) (aget move 1))
              (str (aget move 2) (aget move 3))
              (aget move 4)]))

(defn play-opponent-move [state move]
  (chess/move (:chess state) move)
  (-> state
      (update-in [:chessground] #(chessground.data/api-move-piece % move))
      (update-in [:chessground] #(chessground.data/set-movable-dests % (pp (chess/dests (:chess state)))))
      (update-in [:chessground] #(chessground.data/set-turn-color % (:color state)))
      (dissoc :initial-move)))

(defn play-initial-move [state]
  (play-opponent-move state (:initial-move state)))

(defn play-opponent-next-move [{:keys [lines progress] :as state}]
  (let [move (first (first (get-in lines progress)))]
    (-> state
        (play-opponent-move move)
        (update-in [:progress] #(conj % move)))))

(defn try-move [{:keys [lines progress]} move]
  (let [try-m (fn [m]
                (let [new-progress (conj progress m)
                      new-lines (get-in lines new-progress)]
                  (and new-lines [new-progress new-lines])))
        moves (map #(assoc move 2 %) [nil "q" "n" "r" "b"])
        tries (remove nil? (map try-m moves))]
    (or (first (filter #(not= (second %) "retry") tries))
        (first tries)
        [progress "fail"])))

(defn revert [state]
  (-> state
      (update-in [:chessground] #(chessground.data/with-fen % (.fen (:chess state))))
      (update-in [:chessground] #(chessground.data/set-movable-dests % (chess/dests (:chess state))))))

(defn user-move [state move ctrl]
  (let [[new-progress new-lines] (try-move state move)]
    (case new-lines
      "retry" (-> state
                  revert
                  (assoc :comment :retry))
      "fail" (-> state
                 revert
                 (assoc :comment :fail))
      "win" state
      (do (chess/move (:chess state) move)
          (js/setTimeout #(ctrl :play-opponent-next-move nil) 1000)
          (-> state
              (update-in [:chessground] #(chessground.data/set-turn-color % (:opponent-color state)))
              (assoc :progress new-progress
                     :comment :great))))))

(defn give-up [state]
  (-> state
      (assoc :comment :fail
             :mode "view")))

(defn- parse-lines [lines]
  (if (map? lines)
    (into {} (for [[k v] lines] [(str->move (name k)) (parse-lines v)]))
    lines))

(defn make [config ctrl]
  {:mode (:mode config) ; view | play | try
   :color (:color config)
   :opponent-color (opposite-color (:color config))
   :progress []
   :comment nil ; :fail | :retry | :great
   :initial-move (str->move (:initialMove config))
   :lines (parse-lines (:lines config))
   :user (:user config)
   :difficulty (:difficulty config)
   :urls (:urls config)
   :i18n (:i18n config)
   :chess (let [ch (js/Chess.)]
            (.load ch (:fen config))
            ch)
   :chessground (chessground.api/main
                  {:fen (:fen config)
                   :orientation (:color config)
                   :turnColor (opposite-color (:color config))
                   :movable {:free false
                             :color (:color config)
                             :events {:after #(ctrl :user-move [%1 %2])}}
                   :animation {:enabled true
                               :duration 500}
                   :premovable {:enabled false}})})
