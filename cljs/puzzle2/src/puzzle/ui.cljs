(ns org.lichess.puzzle.ui
  (:require [chessground.common :refer [pp]]
            [chessground.ui :as cg-ui]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent Puzzle [{:keys [cg-obj]} i18n ctrl]
  (d/div {:className "puzzle"}
         (d/div {:className "center"}
          (chessground.ui/board-component (chessground.ui/clj->react cg-obj ctrl)))))

(defn root [app ctrl]
  (Puzzle {:mode (:mode app)
           :cg-obj (:chessground app)}
          (:i18n app)
          ctrl))
