(ns org.lichess.puzzle.ui
  (:require [chessground.common :refer [pp]]
            [chessground.ui :as cg-ui]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent Puzzle [app i18n ctrl]
  (d/div {:className "puzzle"} Puzzle))

(defn root [app ctrl]
  (Puzzle {}
          (:i18n app)
          ctrl))
