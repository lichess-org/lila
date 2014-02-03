(ns lichess.puzzle
  (:require [dommy.core :as dommy]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close!]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn log! [& args] (.log js/console (apply pr-str args)))
(defn log-obj! [obj] (.log js/console obj))

(def static-domain (str "http://" (clojure.string/replace (.-domain js/document) #"^\w+" "static")))
(def chessboard-elem (sel1 "#chessboard"))
(def initial-fen (dommy/attr chessboard-elem "data-fen"))
(def lines (js->clj (js/JSON.parse (dommy/attr chessboard-elem "data-lines"))))
(def drop-chan (chan))
(def animation-delay 300)
(def chess (new js/Chess initial-fen))

(defn playing [] (not (dommy/has-class? (sel1 "#puzzle") "complete")))

(defn make-move
  ([orig, dest] {:from orig :to dest})
  ([move] (let [[a, b, c, d] (seq move)] (make-move (str a b) (str c d)))))

(defn delay-chan [fun duration] (let [ch (chan)] (js/setTimeout #(put! ch (or (fun) true)) duration) ch))
(defn await-chan [value duration] (let [ch (chan)] (js/setTimeout #(put! ch value) duration) ch))
(defn await-in [ch value duration] (js/setTimeout #(put! ch value) duration) ch)

(defn on-drop! [orig, dest]
  (if (and (playing) (.move chess (clj->js (make-move orig dest))))
    (put! drop-chan (str orig dest)) "snapback"))

(def chessboard (new js/ChessBoard "chessboard"
                     (clj->js {:position initial-fen
                               :orientation (dommy/attr chessboard-elem "data-color")
                               :draggable true
                               :dropOffBoard "snapback"
                               :sparePieces false
                               :pieceTheme (str static-domain "/assets/images/chessboard/{piece}.png")
                               :moveSpeed animation-delay
                               :onDrop on-drop!})))

(defn set-position! [fen] (.position chessboard fen))

(defn try-move [progress move]
  (let [new-progress (conj progress move)
        new-lines (get-in lines new-progress)]
    (if new-lines [new-progress new-lines] false)))

(defn ai-play! [branch]
  (let [ch (chan) move (first (first branch))]
    (when-let [valid (.move chess (clj->js (make-move move)))]
      (go
        (set-position! (.fen chess))
        (await-in ch move (+ 50 animation-delay))))
    ch))

(defn end! [result]
  (close! drop-chan)
  (dommy/add-class! (sel1 "#puzzle") (str "complete " result)))

(go (loop [progress [] fen initial-fen]
      (let [move (<! drop-chan)]
        (if-let [[new-progress new-lines] (try-move progress move)]
          (do
            (set-position! (.fen chess))
            (<! (await-chan true (+ animation-delay 50)))
            (if (= new-lines "end")
              (end! "success")
              (let [aim (<! (ai-play! new-lines))]
                (if (= (get new-lines aim) "end")
                  (end! "success")
                  (recur (conj new-progress aim) (.fen chess))))))
          (do
            (.load chess fen)
            (<! (delay-chan #(set-position! fen) animation-delay))
            (<! (await-chan true (+ animation-delay 50)))
            (recur progress fen))))))
