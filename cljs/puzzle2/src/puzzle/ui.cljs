(ns org.lichess.puzzle.ui
  (:require [chessground.common :refer [pp]]
            [chessground.ui :as cg-ui]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [jayq.core :as jq :refer [$]]))

(defn- make-buttons [el] (.disableSelection (.buttonset ($ :.buttons el))))

(q/defcomponent UserInfos [{:keys [rating history]} trans]
  (letfn [(load-chart [el]
            (let [dark (jq/has-class ($ :body) :dark)]
              (.sparkline ($ :.user_chart el)
                          (-> q/*component* .-_owner .-props (aget "value") :history clj->js)
                          #js {:type "line" :width "213px" :height "80px"
                               :lineColor (if dark "#4444ff" "#0000ff")
                               :fillColor (if dark "#222255" "#ccccff")})))]
    (q/wrapper
      (d/div {:className "chart_container"}
             (d/p {} (trans :yourPuzzleRatingX rating))
             (when history
               (d/div {:className "user_chart"} "")))
      :onMount load-chart
      :onUpdate load-chart)))

(q/defcomponent TrainingBox [{:keys [user]} urls trans]
  (d/div {:className "box"}
         (d/h1 {} (trans :training))
         (d/div {:className "tabs buttons"}
                (d/a {:className "button active"
                      :href (:puzzle urls)} "Puzzle")
                (d/a {:className "button"
                      :href (:coordinate urls)} "Coordinate"))
         (when user (UserInfos user trans))))

(q/defcomponent CommentRetry [_ trans]
  (d/div {:className "comment retry"}
         (d/h3 {} (d/strong {} (trans :goodMove)))
         (d/span {} (trans :butYouCanDoBetter))))

(q/defcomponent CommentGreat [_ trans]
  (d/div {:className "comment great"}
         (d/h3 {:data-icon "E"} (d/strong {} (trans :bestMove)))
         (d/span {} (trans :keepGoing))))

(q/defcomponent CommentFail [try-again trans]
  (d/div {:className "comment fail"}
         (d/h3 {:data-icon "k"} (d/strong {} (trans :puzzleFailed)))
         (when try-again (d/span {} (trans :butYouCanKeepTrying)))))

(q/defcomponent Difficulty [{:keys [choices current]} urls trans ctrl]
  (apply d/div {:className "difficulty buttons"}
         (map (fn [[id name]]
                (d/button {:key id
                           :className (when (= id current) "ui-state-active")
                           :disabled (= id current)
                           :onClick #(jq/ajax {:type "POST"
                                               :url (:difficulty urls)
                                               :data {:difficulty id}
                                               :success (partial ctrl :reload)})}
                          name)) choices)))

(q/defcomponent Side [{:keys [commentary mode user difficulty]} urls trans ctrl]
  (d/div {:className "side"}
         (TrainingBox {:user user
                       :difficulty difficulty} urls trans)
         (when difficulty (Difficulty difficulty urls trans ctrl))
         (case commentary
           :retry (CommentRetry nil trans)
           :great (CommentGreat nil trans)
           :fail (CommentFail (= "try" mode) trans)
           commentary)))

(q/defcomponent Player [{:keys [color playing?]} trans]
  (d/div {:className (str "lichess_player " color)}
         (d/div {:className (str "piece king " color)} "")
         (d/p {} (if playing? (trans :yourTurn) (trans :waiting)))))

(q/defcomponent Table [{:keys [color turn-color]} trans ctrl]
  (d/div {:className "table_inner"}
         (d/div {:className "lichess_current_player"}
                (Player {:color turn-color
                         :playing? (= color turn-color)} trans))
         (d/p {:className "findit"} (case color
                                      "white" (trans :findTheBestMoveForWhite)
                                      "black" (trans :findTheBestMoveForBlack)))
         (d/div {:className "lichess_control"}
                (d/a {:className "button"
                      :onClick #(ctrl :give-up nil)} (trans :giveUp)))))

(q/defcomponent Right [props trans ctrl]
  (letfn [(center-right [el]
            (set! (-> el .-style .-top) (str (- 256 (/ (.-offsetHeight el) 2)) "px")))]
    (q/wrapper
      (d/div {:className "right"}
             (d/div {:className "lichess_table onbg"}
                    (Table props trans ctrl)))
      :onMount center-right
      :onUpdate center-right)))

(q/defcomponent History [_ url]
  (q/wrapper
    (d/div {:className "history"} "")
    :onMount (fn [el] (.load ($ el) url))))

(q/defcomponent Puzzle [{:keys [cg-obj color mode commentary user difficulty]} urls trans ctrl]
  (d/div {:id "puzzle"
          :className "training"}
         (Side {:commentary commentary
                :mode mode
                :user user
                :difficulty difficulty} urls trans ctrl)
         (Right {:turn-color (:turn-color cg-obj)
                 :color color} trans ctrl)
         (d/div {:className "center"}
                (chessground.ui/board-component (chessground.ui/clj->react cg-obj ctrl))
                (History {} (:history urls)))))

(defn make-trans [i18n]
  (fn [k & args]
    (reduce (fn [s v] (.replace s "%s" v)) (get i18n k) args)))

(defn root [app ctrl]
  (Puzzle {:mode (:mode app)
           :color (:color app)
           :commentary (:comment app)
           :cg-obj (:chessground app)
           :user (:user app)
           :difficulty (:difficulty app)}
          (:urls app)
          (make-trans (:i18n app))
          ctrl))
