(ns lichess.puzzle.core
  (:require [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]]))

(defn log! [& args] (.log js/console (apply pr-str args)))
(defn log-obj! [obj] (.log js/console obj))

(def $wrap ($ :#puzzle_wrap))
(def chess (new js/Chess))

(defn await-in [ch value duration] (js/setTimeout #(put! ch value) duration) ch)

(defn apply-move
  ([ch orig, dest] (.move ch (clj->js {:from orig :to dest :promotion "q"})))
  ([ch move] (let [[a, b, c, d] (seq move)] (apply-move ch (str a b) (str c d)))))

(defn color-move! [$puzzle move]
  (let [[a b c d] (seq move) [orig dest] [(str a b) (str c d)]]
    (jq/remove-class ($ :.last $puzzle) :last)
    (let [squares (clojure.string/join ", " (map #(str ".square-" %) [orig dest]))]
      (jq/add-class ($ squares $puzzle) :last))))

(defn make-chessboard [config]
  (let [static-domain (str "http://" (clojure.string/replace (.-domain js/document) #"^\w+" "static"))]
    (new js/ChessBoard "chessboard"
         (clj->js (merge {:sparePieces false
                          :pieceTheme (str static-domain "/assets/images/chessboard/{piece}.png")}
                         config)))))

(defn center-right! [$right]
  (jq/css $right {:top (str (- 256 (/ (jq/height $right) 2)) "px")}))

(defn user-chart! [$chart]
  (.sparkline $chart (jq/data $chart :points) (clj->js {:type "line"
                                                        :width "213px"
                                                        :height "80px"})))
