(ns lichess.problem
  (:require [dommy.core :as dommy]
            [cljs.core.async :refer [put! chan <!]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn log! [& args] (.log js/console (apply pr-str args)))
(defn log-obj! [obj] (.log js/console obj))

(def static-domain (str "http://" (clojure.string/replace (.-domain js/document) #"^\w+" "static")))
(def chessboard-elem (sel1 "#chessboard"))
(def initial-fen (dommy/attr chessboard-elem "data-fen"))
(def lines (js->clj (js/JSON.parse (dommy/attr chessboard-elem "data-lines"))))
(def drop-chan (chan))
(def chessboard (new js/ChessBoard "chessboard"
                     (clj->js {:position initial-fen
                               :orientation (dommy/attr chessboard-elem "data-color")
                               :draggable true
                               :dropOffBoard "snapback"
                               :sparePieces false
                               :pieceTheme (str static-domain "/assets/images/chessboard/{piece}.png")
                               :moveSpeed 500
                               :onDrop #(put! drop-chan %&)})))

(defn split-move [move] (clojure.string/replace move #"^(\w{2})(\w{2})" "$1-$2"))

(defn set-position! [fen] (.position chessboard fen true))

(defn try-move [progress move]
  (log! progress move)
  (let [new-progress (conj progress move)
        new-lines (get-in lines new-progress)]
    (if new-lines [new-progress new-lines] false)))

(defn ai-play! [branch]
  (let [move (first (first branch))]
    (.move chessboard (split-move move))
    move))

(defn start! []
  (go (loop [progress [] fen initial-fen]
        (let [[orig, target] (<! drop-chan) move (str orig target)]
          (if-let [[new-progress new-lines] (try-move progress move)]
            (do
              (let [ai-move (ai-play! new-lines)]
                (recur (conj new-progress ai-move) (.position chessboard "fen"))))
            (do
              (set-position! fen)
              (recur progress fen)))))))
(start!)
