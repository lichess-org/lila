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

(defn play-opponent-move [{ch :chess {color :color} :puzzle :as state} move]
  (chess/move ch move)
  (-> state
      (update-in [:chessground] #(chessground.data/api-move-piece % move))
      (update-in [:chessground] #(chessground.data/set-movable-dests % (chess/dests ch)))
      (update-in [:chessground] #(chessground.data/set-turn-color % color))
      (dissoc :initial-move)))

(defn play-initial-move [state]
  (play-opponent-move state (-> state :puzzle :initial-move)))

(defn play-opponent-next-move [{:keys [puzzle progress] :as state}]
  (let [move (first (first (get-in (:lines puzzle) progress)))]
    (-> state
        (play-opponent-move move)
        (update-in [:progress] #(conj % move)))))

(defn try-move [{:keys [puzzle progress]} move]
  (let [try-m (fn [m]
                (let [new-progress (conj progress m)
                      new-lines (get-in (:lines puzzle) new-progress)]
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

(defn user-move [{ch :chess puzzle :puzzle :as state} move ctrl]
  (let [[new-progress new-lines] (try-move state move)]
    (case new-lines
      "retry" (-> state
                  revert
                  (assoc :comment :retry))
      "fail" (-> state
                 revert
                 (assoc :comment :fail))
      "win" state
      (do (chess/move ch move)
          (js/setTimeout #(ctrl :play-opponent-next-move nil) 1000)
          (-> state
              (update-in [:chessground] #(chessground.data/set-turn-color % (:opponent-color puzzle)))
              (assoc :progress new-progress
                     :comment :great))))))

(defn find-best-line [lines]
  (loop [paths (map (fn [p] [p]) (keys lines))]
    (if (empty? paths) '()
      (let [[path & siblings] paths
            ahead (get-in lines path)]
        (case ahead
          "win" path
          "retry" (recur siblings)
          (let [children (map #(conj path %) (keys ahead))]
            (recur (concat siblings children))))))))

(defn find-best-line-from-progress [lines progress]
  (let [ahead (get-in lines progress)]
    (if (= ahead "win")
      progress
      (concat progress (find-best-line ahead)))))

(defn make-history [{:keys [puzzle progress]}]
  (let [line (find-best-line-from-progress (:lines puzzle) progress)
        c (js/Chess. (:fen puzzle))]
    (map (fn [move]
           (chess/move c move)
           [move (.fen c)]) line)))

(defn- parse-lines [lines]
  (if (map? lines)
    (into {} (for [[k v] lines] [(str->move (name k)) (parse-lines v)]))
    lines))

(defn- rename-key [hashmap from to]
  (-> hashmap
      (dissoc from)
      (assoc to (get hashmap from))))

(defn make [config ctrl]
  (let [puzzle (-> (:puzzle config)
                   (rename-key :initialMove :initial-move)
                   (rename-key :initialPly :initial-ply)
                   (rename-key :gameId :game-id)
                   (update-in [:initial-move] str->move)
                   (update-in [:lines] parse-lines)
                   (assoc :opponent-color (opposite-color (get-in config [:puzzle :color]))))
        state {:puzzle puzzle
               :mode (:mode config) ; view | play | try
               :progress []
               :comment nil ; :fail | :retry | :great
               :attempt (rename-key (:attempt config) :userRatingDiff :user-rating-diff)
               :win (:win config)
               :voted (:voted config)
               :started-at (js/Date.)
               :user (:user config)
               :difficulty (:difficulty config)
               :urls (:urls config)
               :i18n (:i18n config)
               :chess (chess/make (:fen puzzle))
               :chessground (chessground.api/main
                              {:fen (:fen puzzle)
                               :orientation (:color config)
                               :turnColor (:opponent-color puzzle)
                               :movable {:free false
                                         :color (:color puzzle)
                                         :events {:after #(ctrl :user-move [%1 %2])}}
                               :animation {:enabled true
                                           :duration 500}
                               :premovable {:enabled false}})}]
    (if (= (:mode state) "view")
      (assoc state :history (make-history state))
      state)))

(defn reload [config ctrl]
  (make (js->clj config :keywordize-keys true) ctrl))

(defn reload-with-progress [state config ctrl]
  (make (assoc
          (js->clj config :keywordize-keys true)
          :progress
          (:progress state)) ctrl))
