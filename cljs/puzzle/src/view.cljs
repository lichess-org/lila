(ns lichess.puzzle.view
  (:require [lichess.puzzle.core :as core :refer [$puzzle $board chess log!]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def browse-chan (async/chan))
(def animation-delay 200)

(defn make-chessboard [fen] (core/make-chessboard {:position fen
                                                   :draggable false}))

(defn bind-vote! [$vote]
  (jq/on $vote :click :button (fn [e]
                                (jq/prevent e)
                                (jq/attr ($ :button $vote) :disabled)
                                (jq/xhr [:post (jq/attr ($ :form $vote) :action)]
                                        {:vote (jq/attr ($ (.-target e)) :value)}
                                        #(jq/html $vote %)))))

(defn bind-browse! [$browse]
  (jq/on $browse :click :button #(put! browse-chan (jq/attr ($ (.-target %)) :value))))

(defn make-history [initial-fen line]
  (let [c (new js/Chess initial-fen)]
    (map (fn [move] (core/apply-move c move) [move (.fen c)]) line)))

(defn run! [initial-step]
  (let [$browse ($ :.prev_next $puzzle)
        $prev ($ :.prev $browse)
        $next ($ :.next $browse)
        initial-fen (jq/data $board :fen)
        chessboard (make-chessboard initial-fen)
        line (clojure.string/split (jq/data $board :flat-line) " ")
        history (vec (make-history initial-fen (conj (seq line) (jq/data $board :move))))]
    (bind-vote! ($ :div.vote_wrap))
    (bind-browse! $browse)
    (go
      (.load chess initial-fen)
      (<! (timeout 1000))
      (loop [step initial-step]
        (let [[move fen] (get history step)
              is-first (= step 0)
              is-last (= step (- (count history) 1))]
          (jq/attr $prev :disabled is-first)
          (jq/attr $next :disabled is-last)
          (.position chessboard fen)
          (core/color-move! move)
          (<! (timeout (+ 50 animation-delay)))
          (let [[browse ch] (alts! [browse-chan])]
            (when (and (= browse "prev") (not is-first)) (recur (- step 1)))
            (when (and (= browse "next") (not is-last)) (recur (+ step 1)))
            (recur step)))))))
