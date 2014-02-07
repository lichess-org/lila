(ns lichess.puzzle.core
  (:require [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]]))

(defn log! [& args] (.log js/console (apply pr-str args)))
(defn log-obj! [obj] (.log js/console obj))

(def $puzzle ($ :#puzzle))
(def $board ($ :#chessboard))
(def mode (keyword (jq/data $puzzle :mode)))
(def lines (js->clj (jq/data $board :lines)))
(def animation-delay 300)
(def chess (new js/Chess))

(defn await-in [ch value duration] (js/setTimeout #(put! ch value) duration) ch)

(defn apply-move
  ([orig, dest] (.move chess (clj->js {:from orig :to dest :promotion "q"})))
  ([move] (let [[a, b, c, d] (seq move)] (apply-move (str a b) (str c d)))))

(defn color-move! [move]
  (let [[a b c d] (seq move) [orig dest] [(str a b) (str c d)]]
    (jq/remove-class ($ :.last $board) :last)
    (let [squares (clojure.string/join ", " (map #(str ".square-" %) [orig dest]))]
      (jq/add-class ($ squares) :last))))

(defn make-chessboard [config]
  (let [static-domain (str "http://" (clojure.string/replace (.-domain js/document) #"^\w+" "static"))]
    (new js/ChessBoard "chessboard"
         (clj->js (merge {:orientation (jq/data $board :color)
                          :sparePieces false
                          :pieceTheme (str static-domain "/assets/images/chessboard/{piece}.png")
                          :moveSpeed animation-delay} config)))))
