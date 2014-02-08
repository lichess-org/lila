(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [$puzzle $board chess mode log!]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [<! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def started-at (new js/Date))
(def drop-chan (async/chan))
(def giveup-chan (async/chan))
(def animation-delay 300)

(defn on-drop! [orig, dest]
  (if (core/apply-move chess orig dest) (put! drop-chan (str orig dest)) "snapback"))

(defn make-chessboard [fen] (core/make-chessboard {:position fen
                                                   :draggable true
                                                   :dropOffBoard "snapback"
                                                   :onDrop on-drop!}))

(defn ai-play! [chessboard branch]
  (let [ch (async/chan) move (first (first branch))]
    (when (core/apply-move chess move)
      (core/color-move! move)
      (go
        (.position chessboard (.fen chess))
        (core/await-in ch move (+ 50 animation-delay))))
    ch))

(defn set-status! [status] (jq/attr $puzzle :class status))

(defn post-attempt! [retries win chan]
  (jq/xhr [:post (jq/data $puzzle :post-url)]
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
  (let [chessboard (make-chessboard (jq/data $board :fen))]
    (go
      (.load chess (jq/data $board :fen))
      (<! (timeout 1000))
      (core/apply-move chess (jq/data $board :move))
      (.position chessboard (.fen chess))
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
                      (<! (timeout animation-delay))
                      (.load chess fen)
                      (.position chessboard fen)
                      (recur progress fen (+ 1 retries)))
            "fail" (do
                     (<! (timeout animation-delay))
                     (.load chess fen)
                     (.position chessboard fen)
                     (if-let [res (<! (fail! retries))]
                       (log! "Move to view mode!!" res)
                       (recur progress fen retries)))
            (do
              (set-status! "playing")
              (core/color-move! move)
              (.position chessboard (.fen chess))
              (<! (timeout (+ animation-delay 50)))
              (if (= new-lines "win")
                (let [res (<! (win! retries))] (log! "Move to view mode!!" res))
                (let [aim (<! (ai-play! chessboard new-lines))]
                  (if (= (get new-lines aim) "win")
                    (let [res (<! (win! retries))] (log! "Move to view mode!!" res))
                    (recur (conj new-progress aim) (.fen chess) retries)))))))))))
