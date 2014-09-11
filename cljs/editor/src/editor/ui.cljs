(ns org.lichess.editor.ui
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [chessground.ui :as cg-ui]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn- str-replace [in pattern replacement flags]
  (.replace in (js/RegExp. pattern flags) replacement))

(defn- make-url [base fen]
  (str base
       (-> fen
           js/encodeURIComponent
           (str-replace "%20" "_" "g")
           (str-replace "%2F" "/" "g"))))

(q/defcomponent FenInput [fen]
  (d/p {}
       (d/strong {:className "name"} "FEN")
       (d/input {:className "copyable fen-string"
                 :readOnly true
                 :spellCheck false
                 :value fen})))

(q/defcomponent UrlInput [fen base-url]
  (d/p {}
       (d/strong {:className "name"} "URL")
       (d/input {:className "copyable permalink"
                 :readOnly true
                 :spellCheck false
                 :value (make-url base-url fen)})))

(q/defcomponent CastleCheckbox [castles label id ctrl reversed]
  (let [input (d/input {:type "checkbox"
                        :defaultChecked (get castles id)
                        :onChange #(ctrl :set-castle [id (-> % .-target .-checked)])})]
    (if reversed
      (d/label {} input label)
      (d/label {} label input))))

(q/defcomponent Controls [{:keys [fen color castles]} i18n base-url ctrl]
  (d/div {:id "editor-side"}
         (d/div {}
                (d/a {:className "button"
                      :onClick #(ctrl :start nil)} (:startPosition i18n))
                (d/a {:className "button"
                      :onClick #(ctrl :clear nil)} (:clearBoard i18n)))
         (d/div {}
                (d/a {:className "button"
                      :data-icon "B"
                      :onClick #(ctrl :toggle-orientation nil)} (str " " (:flipBoard i18n)))
                (d/a {:className "button"
                      :onClick (fn []
                                 (let [fen (js/prompt "Paste FEN position")]
                                   (when (not= "" (.trim fen))
                                     (set! js/window.location (make-url base-url fen)))))}
                     (:loadPosition i18n)))
         (d/div {}
                (d/select {:className "color"
                           :defaultValue color
                           :onChange #(ctrl :set-color (-> % .-target .-value))}
                          (d/option {:value "w"} (:whitePlays i18n))
                          (d/option {:value "b"} (:blackPlays i18n))))
         (d/div {:className "castling"}
                (d/strong {} "Castling")
                (d/div {}
                       (CastleCheckbox castles "White O-O" "K" ctrl false)
                       (CastleCheckbox castles "White O-O-O" "Q" ctrl true))
                (d/div {}
                       (CastleCheckbox castles "Black O-O" "k" ctrl false)
                       (CastleCheckbox castles "Black O-O-O" "k" ctrl true)))
         (d/div {}
                (d/a {:className "button"
                      :href (str "/?fen=" fen "#ai")} (:playWithTheMachine i18n))
                (d/a {:className "button"
                      :href (str "/?fen=" fen "#friend")} (:playWithAFriend i18n)))))

(q/defcomponent Editor [{:keys [fen color castles cg-obj]} i18n base-url ctrl]
  (d/div {:className "editor"}
         (chessground.ui/board-component (chessground.ui/clj->react cg-obj ctrl))
         (Controls {:fen fen
                    :color color
                    :castles castles}
                   i18n base-url ctrl)
         (d/div {:className "copyables"}
                (FenInput fen)
                (UrlInput fen base-url))))

(defn root [app ctrl]
  (Editor {:fen (:fen app)
           :color (:color app)
           :castles (:castles app)
           :cg-obj (:chessground app)}
          (:i18n app)
          (:base-url app)
          ctrl))
