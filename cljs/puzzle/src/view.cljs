(ns lichess.puzzle.view
  (:require [lichess.puzzle.core :as core :refer [$puzzle $board]]
            [jayq.core :as jq :refer [$]]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def $prev ($ [$puzzle :.prev]))
(def $next ($ [$puzzle :.next]))
(def $vote ($ :div.vote_wrap))

(defn view-chessboard [fen]
  (core/make-chessboard {:position fen :draggable false}))

(defn bind-vote! []
  (jq/on $vote :click :button (fn [e]
                                (jq/prevent e)
                                (jq/attr ($ :button $vote) :disabled)
                                (jq/xhr [:post (jq/attr ($ :form $vote) :action)]
                                        {:vote (jq/attr ($ (.-target e)) :value)}
                                        #(jq/html $vote %)))))

(defn run! []
  (bind-vote!))
