(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [chess log!]]
            [lichess.puzzle.view :as view]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [<! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def drop-chan (async/chan))
(def giveup-chan (async/chan))
(def animation-delay 300)
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
  (let [color (if (= (.turn chess) "w") "white" "black")]
    (jq/fade-out ($ :.lichess_player $puzzle) animation-delay)
    (jq/fade-in ($ (str ".lichess_player." color) $puzzle) animation-delay)))

(defn try-move [lines progress move]
  (let [try-m (fn [m]
                (let [new-progress (conj progress m) new-lines (get-in lines new-progress)]
                  (and new-lines [new-progress new-lines])))
        moves (map #(str move %) ["" "q" "n" "r" "b"])
        tries (remove nil? (map try-m moves))
        search #(first (filter % tries))]
    (or (search #(not= % "retry")) (search #(true)) [progress "fail"])))

(defn ai-play! [$puzzle chessboard branch]
  (let [ch (async/chan) move (first (first branch))]
    (when (core/apply-move chess move)
      (core/color-move! $puzzle move)
      (go
        (.position chessboard (.fen chess))
        (core/await-in ch move animation-delay-plus)))
    ch))

(defn set-status! [$puzzle status] (jq/attr $puzzle :class status))

(defn post-attempt! [$puzzle win started-at]
  (let [chan (async/chan)]
    (jq/xhr [:post (jq/data $puzzle :post-url)]
            {:win win :time (- (.getTime (new js/Date)) (.getTime started-at))}
            #(put! chan %))
    chan))

(defn get-view [$puzzle]
  (let [chan (async/chan)]
    (jq/xhr [:get (jq/data $puzzle :view-url)] {} #(put! chan %))
    chan))

(defn win! [$puzzle mode progress started-at]
  (go
    (let [res (<! (if (= mode :play) (post-attempt! $puzzle 1 started-at) (get-view $puzzle)))]
      (<! (timeout animation-delay))
      (jq/html core/$wrap res)
      (view/run! progress))))

(defn fail! [$puzzle mode started-at]
  (set-status! $puzzle "playing fail")
  (if (= mode :play) (post-attempt! $puzzle 0 started-at) (timeout 10)))

(defn run! []
  (let [$puzzle ($ :#puzzle)
        mode (keyword (jq/data $puzzle :mode))
        lines (js->clj (jq/data $puzzle :lines))
        initial-fen (jq/data $puzzle :fen)
        chessboard (make-chessboard $puzzle initial-fen)
        started-at (new js/Date)]
    (core/center-right! ($ :.right $puzzle))
    (core/user-chart! ($ :.user_chart $puzzle))
    (jq/bind ($ :.giveup $puzzle) :click #(put! giveup-chan %))
    (go
      (.load chess initial-fen)
      (<! (timeout 1000))
      (core/apply-move chess (jq/data $puzzle :move))
      (.position chessboard (.fen chess))
      (core/color-move! $puzzle (jq/data $puzzle :move))
      (set-status! $puzzle "playing")
      (loop [progress []
             fen (.fen chess)]
        (show-turn! $puzzle)
        (let [[move ch] (alts! [drop-chan giveup-chan])]
          (if (= ch giveup-chan)
            (let [res (<! (post-attempt! $puzzle 0 started-at))]
              (jq/html core/$wrap res)
              (view/run! progress))
            (let [[new-progress new-lines] (try-move lines progress move)]
              (case new-lines
                "retry" (do
                          (set-status! $puzzle "playing retry")
                          (<! (timeout animation-delay))
                          (.load chess fen)
                          (.position chessboard fen)
                          (recur progress fen))
                "fail" (do
                         (<! (timeout animation-delay))
                         (.load chess fen)
                         (.position chessboard fen)
                         (if-let [res (<! (fail! $puzzle mode started-at))]
                           (do
                             (<! (timeout animation-delay))
                             (jq/html core/$wrap res)
                             (view/run! progress))
                           (recur progress fen)))
                (do
                  (set-status! $puzzle "playing great")
                  (core/color-move! $puzzle move)
                  (.position chessboard (.fen chess))
                  (show-turn! $puzzle)
                  (<! (timeout animation-delay-plus))
                  (if (= new-lines "win")
                    (win! $puzzle mode new-progress started-at)
                    (let [aim (<! (ai-play! $puzzle chessboard new-lines))
                          new-new-progress (conj new-progress aim)]
                      (if (= (get new-lines aim) "win")
                        (win! $puzzle mode new-new-progress started-at )
                        (recur new-new-progress (.fen chess))))))))))))))
