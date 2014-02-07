(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [$puzzle $board chess mode log!]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [<! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def started-at (new js/Date))
(def drop-chan (async/chan))
(def giveup-chan (async/chan))

(defn on-drop! [orig, dest]
  (if (core/apply-move orig dest) (put! drop-chan (str orig dest)) "snapback"))

(def play-chessboard
  (core/make-chessboard {:draggable true
                         :dropOffBoard "snapback"
                         :onDrop on-drop!}))

(defn ai-play! [branch]
  (let [ch (async/chan) move (first (first branch))]
    (when (core/apply-move move)
      (core/color-move! move)
      (go
        (.position play-chessboard (.fen chess))
        (core/await-in ch move (+ 50 core/animation-delay))))
    ch))

(defn set-status! [status] (jq/attr $puzzle :class status))

(defn post-attempt! [retries win chan]
  (jq/xhr [:post (jq/data $board :post-url)]
          {:win win :hints 0 :retries retries
           :time (- (.getTime (new js/Date)) (.getTime started-at))}
          #(put! chan %)))

(defn get-view! [chan] (jq/xhr [:get (jq/data $board :view-url)] {} #(put! chan %)))

(defn win! [retries]
  (set-status! "win")
  (let [chan (async/chan)]
    (if (= mode :play) (post-attempt! retries 1 chan) (get-view! chan))
    chan))

(defn fail! [retries]
  (set-status! "playing fail")
  (let [chan (async/chan)]
    (if (= mode :play) (post-attempt! retries 0 chan) (put! chan false))
    chan))

(defn run! []
  (jq/bind ($ :.giveup $puzzle) :click #(put! giveup-chan %))
  (go
    (.load chess (jq/data $board :fen))
    (.position play-chessboard (jq/data $board :fen))
    (<! (timeout 1000))
    (core/apply-move (jq/data $board :move))
    (.position play-chessboard (.fen chess))
    (core/color-move! (jq/data $board :move))
    (set-status! "playing")
    (loop [progress []
           fen (.fen chess)
           retries 0]
      (let [[move ch] (alts! [drop-chan giveup-chan])
            new-progress (conj progress move)
            new-lines (or (get-in core/lines new-progress) "fail")]
        (case new-lines
          "retry" (do
                    (set-status! "playing retry")
                    (<! (timeout core/animation-delay))
                    (.load chess fen)
                    (.position play-chessboard fen)
                    (recur progress fen (+ 1 retries)))
          "fail" (do
                   (<! (timeout core/animation-delay))
                   (.load chess fen)
                   (.position play-chessboard fen)
                   (if-let [res (<! (fail! retries))]
                     (log! "Move to view mode!!" res)
                     (recur progress fen retries)))
          (do
            (set-status! "playing")
            (core/color-move! move)
            (.position play-chessboard (.fen chess))
            (<! (timeout (+ core/animation-delay 50)))
            (if (= new-lines "win")
              (let [res (<! (win! retries))] (log! "Move to view mode!!" res))
              (let [aim (<! (ai-play! new-lines))]
                (if (= (get new-lines aim) "win")
                  (let [res (<! (win! retries))] (log! "Move to view mode!!" res))
                  (recur (conj new-progress aim) (.fen chess) retries))))))))))
