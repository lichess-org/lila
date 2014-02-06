(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [$puzzle $board chess]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def started-at (new js/Date))
(def drop-chan (chan))

(defn on-drop! [orig, dest]
  (if (core/apply-move orig dest) (put! drop-chan (str orig dest)) "snapback"))

(def play-chessboard
  (core/make-chessboard {:draggable true
                         :dropOffBoard "snapback"
                         :onDrop on-drop!}))

(defn ai-play! [branch]
  (let [ch (chan) move (first (first branch))]
    (when (core/apply-move move)
      (core/color-move! move)
      (go
        (.position play-chessboard (.fen chess))
        (core/await-in ch move (+ 50 core/animation-delay))))
    ch))

(defn set-status! [status] (jq/attr $puzzle :class status))

(defn post-attempt! [retries win]
  (jq/ajax {:url (jq/attr $board :data-post-url)
            :method :post
            :success core/log!
            :data {:win win
                   :hints 0
                   :retries retries
                   :time (- (.getTime (new js/Date)) (.getTime started-at))}}))

(defn win! [retries]
  (set-status! "win")
  (post-attempt! retries 1))

(defn run! []
  (go
    (.load chess (jq/attr $board :data-fen))
    (<! (timeout 1000))
    (core/apply-move (jq/attr $board :data-move))
    (.position play-chessboard (.fen chess))
    (core/color-move! (jq/attr $board :data-move))
    (set-status! "playing")
    (loop [progress []
           fen (.fen chess)
           retries 0
           failed false]
      (let [move (<! drop-chan)
            new-progress (conj progress move)
            new-lines (get-in core/lines new-progress)]
        (case new-lines
          "retry" (do
                    (set-status! "playing retry")
                    (<! (timeout core/animation-delay))
                    (.load chess fen)
                    (.position play-chessboard fen)
                    (recur progress fen (+ 1 retries) failed))
          nil (do
                (when (not failed) (post-attempt! retries 0))
                (set-status! "playing fail")
                (<! (timeout core/animation-delay))
                (.load chess fen)
                (.position play-chessboard fen)
                (recur progress fen retries true))
          (do
            (set-status! "playing")
            (core/color-move! move)
            (.position play-chessboard (.fen chess))
            (<! (timeout (+ core/animation-delay 50)))
            (if (= new-lines "win")
              (win! retries)
              (let [aim (<! (ai-play! new-lines))]
                (if (= (get new-lines aim) "win")
                  (win! retries)
                  (recur (conj new-progress aim) (.fen chess) retries failed))))))))))
