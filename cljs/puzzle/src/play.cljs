(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [chess]]
            [lichess.puzzle.view :as view]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [<! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def drop-chan (async/chan))
(def giveup-chan (async/chan))
(def animation-delay 250)
(def animation-delay-plus (+ 50 animation-delay))

(defn on-drop! [orig, dest]
  (if (core/apply-move chess orig dest) (put! drop-chan (str orig dest)) "snapback"))

(defn make-chessboard [$puzzle fen]
  (core/make-chessboard {:orientation (jq/data $puzzle :color)
                         :position fen
                         :moveSpeed animation-delay
                         :draggable true
                         :dropOffBoard "snapback"
                         :onDrop on-drop!}))

(defn show-turn! [$puzzle]
  (let [color (if (= (.turn chess) "w") "white" "black")
        $table ($ :.table_inner $puzzle)]
    (when (not (jq/has-class $table color))
      (-> $table (jq/remove-class "white black") (jq/add-class color))
      (jq/fade-out ($ :.lichess_player $table) animation-delay)
      (jq/fade-in ($ (str ".lichess_player." color) $table) animation-delay))))

(defn try-move [lines progress move]
  (let [try-m (fn [m]
                (let [new-progress (conj progress m) new-lines (get-in lines new-progress)]
                  (and new-lines [new-progress new-lines])))
        moves (map #(str move %) ["" "q" "n" "r" "b"])
        tries (remove nil? (map try-m moves))]
    (or (first (filter #(not= (second %) "retry") tries))
        (first tries)
        [progress "fail"])))

(defn ai-play! [$puzzle chessboard branch]
  (let [ch (async/chan) move (first (first branch))]
    (when (core/apply-move chess move)
      (core/color-move! $puzzle move)
      (go
        (.position chessboard (.fen chess))
        (core/await-in ch move animation-delay-plus)))
    ch))

(defn set-status! [$puzzle status] (jq/attr $puzzle :class (str "training " status)))

(defn post-attempt! [$puzzle win started-at]
  (let [chan (async/chan)]
    (jq/xhr [:post (jq/data $puzzle :post-url)]
            {:win win :time (- (.getTime (new js/Date)) (.getTime started-at))}
            #(put! chan %))
    chan))

(defn retry! [$puzzle chessboard fen]
  (set-status! $puzzle "playing retry")
  (let [chan (async/chan)]
    (go
      (<! (timeout animation-delay))
      (.load chess fen)
      (.position chessboard fen)
      (put! chan true))
    chan))

(defn win! [$puzzle progress started-at]
  (go
    (let [_ (core/loading! ($ :.giveup $puzzle))
          xhr-chan (post-attempt! $puzzle 1 started-at)
          wait-chan (timeout animation-delay-plus)
          res (<! xhr-chan)
          _ (<! wait-chan)]
      (jq/html core/$wrap res)
      (view/run! progress))))

(defn fail! [$puzzle chessboard fen progress started-at]
  (set-status! $puzzle "playing fail")
  (let [chan (async/chan)]
    (go
      (<! (timeout animation-delay-plus))
      (.load chess fen)
      (.position chessboard fen)
      (if (= (keyword (jq/data $puzzle :mode)) :play)
        (let [_ (core/loading! ($ :.giveup $puzzle))
              xhr-chan (post-attempt! $puzzle 0 started-at)
              wait-chan (timeout animation-delay-plus)
              res (<! xhr-chan)
              _ (<! wait-chan)]
          (jq/html core/$wrap res)
          (view/run! progress)
          (put! chan false))
        (put! chan true)))
    chan))

(defn give-up! [$puzzle progress started-at]
  (set-status! $puzzle "playing fail")
  (core/loading! ($ :.giveup $puzzle))
  (go
    (let [res (<! (post-attempt! $puzzle 0 started-at))]
      (jq/html core/$wrap res)
      (view/run! progress))))

(defn run! []
  (let [$puzzle ($ :#puzzle)
        lines (js->clj (jq/data $puzzle :lines))
        initial-fen (jq/data $puzzle :fen)
        chessboard (make-chessboard $puzzle initial-fen)
        started-at (new js/Date)]
    (core/center-right! ($ :.right $puzzle))
    (core/board-marks! $puzzle)
    (core/buttons! $puzzle)
    (core/user-chart! ($ :.user_chart $puzzle))
    (jq/bind ($ :.giveup $puzzle) :click #(put! giveup-chan %))
    (go
      (.load chess initial-fen)
      (<! (timeout 1000))
      (core/apply-move chess (jq/data $puzzle :move))
      (.position chessboard (.fen chess))
      (core/color-move! $puzzle (jq/data $puzzle :move))
      (set-status! $puzzle "playing")
      (loop [progress [] fen (.fen chess)]
        (show-turn! $puzzle)
        (let [[move ch] (alts! [drop-chan giveup-chan])]
          (if (= ch giveup-chan)
            (give-up! $puzzle progress started-at)
            (let [[new-progress new-lines] (try-move lines progress move)]
              (case new-lines
                "retry" (do (<! (retry! $puzzle chessboard fen))
                            (recur progress fen))
                "fail" (when (<! (fail! $puzzle chessboard fen progress started-at))
                         (recur progress fen))
                (do
                  (set-status! $puzzle "playing great")
                  (let [full-move (last new-progress)
                        prom (get (vec full-move) 4)]
                    ; hack to handle under-promotion http://en.l.org/training/4036
                    (when (and prom (not= prom "q")) (.undo chess) (core/apply-move chess full-move))
                    (core/color-move! $puzzle move)
                    (.position chessboard (.fen chess))
                    (show-turn! $puzzle)
                    (<! (timeout animation-delay-plus))
                    (if (= new-lines "win")
                      (win! $puzzle new-progress started-at)
                      (let [aim (<! (ai-play! $puzzle chessboard new-lines))
                            new-new-progress (conj new-progress aim)]
                        (if (= (get new-lines aim) "win")
                          (win! $puzzle new-new-progress started-at )
                          (recur new-new-progress (.fen chess)))))))))))))))
