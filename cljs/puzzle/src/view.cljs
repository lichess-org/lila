(ns lichess.puzzle.view
  (:require [lichess.puzzle.core :as core :refer [chess log!]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def browse-chan (async/chan))
(def continue-chan (async/chan))
(def animation-delay 200)

(defn make-chessboard [$puzzle]
  (core/make-chessboard {:orientation (jq/data $puzzle :color)
                         :moveSpeed animation-delay
                         :draggable false}))

(defn bind-vote! [$vote]
  (jq/on $vote :click ".enabled a:not(.active)"
         (fn [e] (let [$a ($ (.-target e))]
                   (jq/add-class $a :active)
                   (jq/xhr [:post (jq/data (jq/parent $a) :post-url)]
                           {:vote (jq/data $a :vote)}
                           #(jq/html $vote %))))))

(defn bind-browse! [$browse]
  (jq/on $browse :click :button #(put! browse-chan (jq/attr ($ (.-target %)) :value))))

(defn bind-continue! [$continue]
  (jq/bind $continue :click #(put! continue-chan true)))

(defn make-history [initial-fen line]
  (let [c (new js/Chess initial-fen)]
    (map (fn [move] (core/apply-move c move) [move (.fen c)]) line)))

(defn find-best-line [lines]
  (loop [paths (map (fn [p] [p]) (keys lines))]
    (if (empty? paths) '()
      (let [[path & siblings] paths
            ahead (get-in lines path)]
        (case ahead
          "win" path
          "retry" (recur siblings)
          (let [children (map #(conj path %) (keys ahead))]
            (recur (concat siblings children))))))))

(defn find-best-line-from-progress [lines progress]
  (let [ahead (get-in lines progress)]
    (if (= ahead "win") progress (concat progress (find-best-line ahead)))))

(defn get-new [$puzzle]
  (let [chan (async/chan)]
    (jq/xhr [:get (jq/data $puzzle :new-url)] {} #(put! chan %))
    chan))

(defn play-new! [$puzzle]
  (core/loading! ($ :button.continue $puzzle))
  (go
    (let [res (<! (get-new $puzzle))]
      (jq/html core/$wrap res)
      (lichess.puzzle.play/run!))))

(defn run! [progress]
  (let [$puzzle ($ :#puzzle)
        $browse ($ :.prev_next $puzzle)
        $prev ($ :.prev $browse)
        $next ($ :.next $browse)
        lines (js->clj (jq/data $puzzle :lines))
        line (find-best-line-from-progress lines progress)
        history (vec (make-history (jq/data $puzzle :fen) (conj (seq line) (jq/data $puzzle :move))))
        chessboard (make-chessboard $puzzle)]
    (core/center-right! ($ :.right $puzzle))
    (core/user-chart! ($ :.user_chart $puzzle))
    (bind-vote! ($ :div.vote_wrap $puzzle))
    (bind-continue! ($ :button.continue $puzzle))
    (bind-browse! $browse)
    (go
      (loop [step (count progress) animate false]
        (let [[move fen] (get history step)
              is-first (= step 0)
              is-last (= step (- (count history) 1))]
          (jq/attr $prev :disabled is-first)
          (jq/attr $next :disabled is-last)
          (.position chessboard fen animate)
          (core/color-move! $puzzle move)
          (<! (timeout (+ 50 animation-delay)))
          (let [[browse ch] (alts! [browse-chan continue-chan])]
            (if (= ch continue-chan)
              (play-new! $puzzle)
              (do
                (when (and (= browse "prev") (not is-first)) (recur (- step 1) true))
                (when (and (= browse "next") (not is-last)) (recur (+ step 1) true))
                (recur step false)))))))))
