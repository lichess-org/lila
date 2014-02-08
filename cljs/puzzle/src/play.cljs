(ns lichess.puzzle.play
  (:require [lichess.puzzle.core :as core :refer [chess log!]]
            [lichess.puzzle.view :as view]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [<! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def started-at (new js/Date))
(def drop-chan (async/chan))
(def giveup-chan (async/chan))
(def animation-delay 300)

(defn on-drop! [orig, dest]
  (if (core/apply-move chess orig dest) (put! drop-chan (str orig dest)) "snapback"))

(defn make-chessboard [$puzzle fen]
  (core/make-chessboard {:orientation (jq/data $puzzle :color)
                         :position fen
                         :moveSpeed animation-delay
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

(defn set-status! [$puzzle status] (jq/attr $puzzle :class status))

(defn post-attempt! [$puzzle retries win]
  (let [chan (async/chan)]
    (jq/xhr [:post (jq/data $puzzle :post-url)]
            {:win win :hints 0 :retries retries
             :time (- (.getTime (new js/Date)) (.getTime started-at))}
            #(put! chan %))
    chan))

(defn get-view! [$puzzle]
  (let [chan (async/chan)]
    (jq/xhr [:get (jq/data $puzzle :view-url)] {} #(put! chan %))
    chan))

(defn win! [$puzzle mode retries]
  (set-status! $puzzle "win")
  (if (= mode :play) (post-attempt! $puzzle retries 1) (get-view! $puzzle)))

(defn fail! [$puzzle mode retries]
  (set-status! $puzzle "playing fail")
  (if (= mode :play) (post-attempt! $puzzle retries 0) (timeout 10)))

(defn run! []
  (let [$puzzle ($ :#puzzle)
        mode (keyword (jq/data $puzzle :mode))
        lines (js->clj (jq/data $puzzle :lines))
        initial-fen (jq/data $puzzle :fen)
        chessboard (make-chessboard $puzzle initial-fen)]
    (jq/bind ($ :.giveup $puzzle) :click #(put! giveup-chan %))
    (go
      (.load chess initial-fen)
      (<! (timeout 1000))
      (core/apply-move chess (jq/data $puzzle :move))
      (.position chessboard (.fen chess))
      (core/color-move! (jq/data $puzzle :move))
      (set-status! $puzzle "playing")
      (loop [progress []
             fen (.fen chess)
             retries 0]
        (let [[move ch] (alts! [drop-chan giveup-chan])]
          (if (= ch giveup-chan)
            (let [res (<! (post-attempt! $puzzle retries 0))]
              (jq/html core/$wrap res)
              (view/run! 0))
            (let [new-progress (conj progress move)
                  new-lines (or (get-in core/lines new-progress) "fail")]
              (case new-lines
                "retry" (do
                          (set-status! $puzzle "playing retry")
                          (<! (timeout animation-delay))
                          (.load chess fen)
                          (.position chessboard fen)
                          (recur progress fen (+ 1 retries)))
                "fail" (do
                         (<! (timeout animation-delay))
                         (.load chess fen)
                         (.position chessboard fen)
                         (if-let [res (<! (fail! $puzzle mode retries))]
                           (do
                             (jq/html core/$wrap res)
                             (view/run! 0))
                           (recur progress fen retries)))
                (do
                  (set-status! $puzzle "playing")
                  (core/color-move! move)
                  (.position chessboard (.fen chess))
                  (<! (timeout (+ animation-delay 50)))
                  (if (= new-lines "win")
                    (let [res (<! (win! $puzzle mode retries))] (log! "Move to view mode!!" res))
                    (let [aim (<! (ai-play! chessboard new-lines))]
                      (if (= (get new-lines aim) "win")
                        (let [res (<! (win! $puzzle mode retries))] (log! "Move to view mode!!" res))
                        (recur (conj new-progress aim) (.fen chess) retries)))))))))))))
