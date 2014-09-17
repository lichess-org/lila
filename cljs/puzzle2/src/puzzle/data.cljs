(ns org.lichess.puzzle.data
  (:require [chessground.common :refer [pp opposite-color]]
            [chessground.data :as cg-data]
            [chessground.common :as cg-common]
            [chessground.api :as cg-api]
            [org.lichess.puzzle.xhr :as xhr]
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
  (update-in state [:chessground] #(-> %
                                       (chessground.data/api-move-piece move)
                                       (chessground.data/with-fen (.fen ch))
                                       (chessground.data/set-movable-dests (chess/dests ch))
                                       (chessground.data/set-turn-color color)
                                       chessground.data/play-premove)))

(defn play-initial-move [state]
  (-> state
      (play-opponent-move (-> state :puzzle :initial-move))
      (assoc :started-at (js/Date.))))

(defn play-opponent-next-move [{:keys [puzzle progress] :as state}]
  (let [move (first (first (get-in (:lines puzzle) progress)))
        new-state (-> state
                      (play-opponent-move move)
                      (update-in [:progress] #(conj % move)))
        new-lines (get-in (:lines puzzle) (:progress new-state))]
    (when (= new-lines "win") (xhr/attempt new-state true))
    new-state))

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
      (update-in [:chessground]
                 #(-> %
                      (chessground.data/with-fen (.fen (:chess state)))
                      (chessground.data/set-movable-dests (chess/dests (:chess state)))
                      (chessground.data/set-last-move (chess/get-last-move (:chess state)))))))

(defn user-finalize-move [{ch :chess puzzle :puzzle :as state} move new-progress]
  (chess/move ch move)
  (-> state
      (assoc :comment :great :progress new-progress)
      (update-in [:chessground] #(-> %
                                     (chessground.data/with-fen (.fen ch))
                                     (chessground.data/set-turn-color (:opponent-color puzzle))))))

(defn user-move [{:keys [puzzle ctrl mode] :as state} move]
  (let [[new-progress new-lines] (try-move state move)]
    (case new-lines
      "retry" (do (js/setTimeout #(ctrl :revert (:id puzzle)) 500)
                  (assoc state :comment :retry))
      "fail" (do (if (= mode "play")
                   (xhr/attempt state false)
                   (js/setTimeout #(ctrl :revert (:id puzzle)) 500))
                 (assoc state :comment :fail))
      "win" (let [new-state (user-finalize-move state move new-progress)]
              (xhr/attempt new-state true)
              new-state)
      (let [new-state (user-finalize-move state move new-progress)]
        (js/setTimeout #(ctrl :play-opponent-next-move (:id puzzle)) 1000)
        new-state))))

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
      (seq progress)
      (concat progress (find-best-line ahead)))))

(defn make-history [{:keys [puzzle progress]}]
  (let [line (find-best-line-from-progress (:lines puzzle) progress)
        c (js/Chess. (:fen puzzle))]
    (vec (map (fn [move]
                (chess/move c move)
                [move (.fen c)]) (conj line (:initial-move puzzle))))))

(defn jump [{:keys [replay] :as state} f-to]
  (let [step (-> replay :step f-to (max 0) (min (-> state :replay :history count dec)))
        [move fen] (get-in replay [:history step])]
    (-> state
        (assoc-in [:replay :step] step)
        (update-in [:chessground] #(chessground.data/with-fen % fen))
        (update-in [:chessground] #(chessground.data/set-last-move % move)))))

(defn initiate [{:keys [mode puzzle progress ctrl] :as state}]
  (if (#{"play" "try"} mode)
    (do (js/setTimeout #(ctrl :play-initial-move (:id puzzle)) 1000)
        state)
    (let [history (make-history state)]
      (-> state
          (assoc :replay {:step 0 :history history})
          (jump #(count progress))))))

(defn set-votes [state [user-vote puzzle-vote]]
  (-> state
      (assoc-in [:attempt :vote] user-vote)
      (assoc-in [:puzzle :vote] puzzle-vote)))

(defn- parse-lines [lines]
  (if (map? lines)
    (into {} (for [[k v] lines] [(str->move (name k)) (parse-lines v)]))
    lines))

(defn- rename-key [hashmap from to]
  (-> hashmap
      (dissoc from)
      (assoc to (get hashmap from))))

(defn make [config ctrl router trans]
  (let [puzzle (-> (:puzzle config)
                   (rename-key :initialMove :initial-move)
                   (rename-key :initialPly :initial-ply)
                   (rename-key :gameId :game-id)
                   (update-in [:initial-move] str->move)
                   (update-in [:lines] parse-lines)
                   (assoc :opponent-color (opposite-color (get-in config [:puzzle :color]))))]
    {:puzzle puzzle
     :mode (:mode config) ; view | play | try
     :progress []
     :comment nil ; :fail | :retry | :great
     :attempt (rename-key (:attempt config) :userRatingDiff :user-rating-diff)
     :win (:win config)
     :voted (:voted config)
     :user (:user config)
     :difficulty (:difficulty config)
     :ctrl ctrl
     :router router
     :trans trans
     :started-at (js/Date.)
     :chess (chess/make (:fen puzzle))
     :chessground (chessground.api/main
                    {:fen (:fen puzzle)
                     :orientation (:color puzzle)
                     :turnColor (:opponent-color puzzle)
                     :movable {:free false
                               :color (when (#{"play" "try"} (:mode config))
                                        (:color puzzle))
                               :events {:after #(ctrl :user-move [%1 %2])}}
                     :animation {:enabled true
                                 :duration 200}
                     :premovable {:enabled true}})}))

(defn reload [state config]
  (-> config
      (js->clj :keywordize-keys true)
      (make (:ctrl state) (:router state) (:trans state))))

(defn reload-with-progress [state config]
  (assoc (reload state config)
         :progress (:progress state)))
