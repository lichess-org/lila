(ns org.lichess.editor.ui
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [chessground.ui :as cg-ui]))

(def ^:private dom (.-DOM js/React))

(def ^:private div (.-div dom))

(def editor-component
  (js/React.createClass
    #js
    {:displayName "Editor"
     :render
     (fn []
       (this-as this
                (let [props (.-props this)]
                  (pp props)
                  (js/console.log (aget props "chessground"))
                  (div #js {:className "editor"} ; won't work without the class name D:
                    (pp (chessground.ui/board-component (aget props "chessground")))))))
     }))

(defn root [app ctrl]
  (editor-component
    #js {:chessground (chessground.ui/clj->react (:chessground app) ctrl)}))
