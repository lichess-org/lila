(ns org.lichess.puzzle.ui
  (:require [chessground.common :refer [pp]]
            [chessground.ui :as cg-ui]
            [chessground.fen :as cg-fen]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [jayq.core :as jq :refer [$]]))

(defn- make-buttons [el] (.disableSelection (.buttonset ($ :.buttons el))))

(defn- show-number [n] (if (> n 0) (str "+" n) n))

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
               (d/div {:className "user_chart"})))
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

(q/defcomponent RatingDiff [diff]
  (d/strong {:className "rating"} (show-number diff)))

(q/defcomponent CommentWin [attempt trans]
  (d/div {:className "comment win"}
         (d/h3 {:data-icon "E"}
               (d/strong {} (trans :victory))
               (when attempt (RatingDiff (:userRatingDiff attempt))))
         (when attempt (d/span {} (trans :puzzleSolvedInXSeconds (:seconds attempt))))))

(q/defcomponent CommentLoss [attempt trans]
  (d/div {:className "comment loss"}
         (d/h3 {:data-icon "E"}
               (d/strong {} (trans :puzzleFailed))
               (when attempt (RatingDiff (:userRatingDiff attempt))))))

(q/defcomponent Difficulty [{:keys [choices current]} ctrl]
  (apply d/div {:className "difficulty buttons"}
         (map (fn [[id name]]
                (d/button {:key id
                           :className (when (= id current) "ui-state-active")
                           :disabled (= id current)
                           :onClick #(ctrl :set-difficulty id)}
                          name)) choices)))

(q/defcomponent Side [{:keys [commentary mode win attempt user difficulty]} urls trans ctrl]
  (d/div {:className "side"}
         (TrainingBox {:user user
                       :difficulty difficulty} urls trans)
         (when difficulty (Difficulty difficulty ctrl))
         (case commentary
           :retry (CommentRetry nil trans)
           :great (CommentGreat nil trans)
           :fail (CommentFail (= "try" mode) trans)
           commentary)
         (case win
           true (CommentWin nil trans)
           false (CommentLoss nil trans)
           (case (:win attempt)
             true (CommentWin attempt trans)
             false (CommentLoss attempt trans)
             ""))))

(q/defcomponent PlayTable [{:keys [color turn-color]} trans ctrl]
  (d/div {:className "lichess_table onbg"}
         (d/div {:className "table_inner"}
                (d/div {:className "lichess_current_player"}
                       (d/div {:className (str "lichess_player " turn-color)}
                              (d/div {:className (str "piece king " turn-color)})
                              (d/p {} (if (= color turn-color) (trans :yourTurn) (trans :waiting)))))
                (d/p {:className "findit"} (case color
                                             "white" (trans :findTheBestMoveForWhite)
                                             "black" (trans :findTheBestMoveForBlack)))
                (d/div {:className "lichess_control"}
                       (d/a {:className "button"
                             :onClick #(ctrl :give-up nil)} (trans :giveUp))))))

(q/defcomponent Vote [{:keys [puzzle attempt]} trans ctrl]
  (d/div {:className (str "upvote" (when attempt " enabled"))}
         (d/a {:title (trans :thisPuzzleIsCorrect)
               :data-icon "S"
               :className (str "upvote" (when (:vote attempt) " active"))})
         (d/span {:className "count hint--bottom"
                  :data-hint "Popularity"} (-> puzzle :vote :sum))
         (d/a {:title (trans :thisPuzzleIsWrong)
               :data-icon "R"
               :className (str "downvote" (when (= (:vote attempt) false) " active"))})))

(q/defcomponent ViewTable [{:keys [puzzle voted attempt auth?]} trans ctrl]
  (d/div {}
         (when (and (:enabled puzzle)
                    (= voted false))
           (d/div {:className "please_vote"}
                  (d/p {:className "first"}
                       (d/strong {} (trans :wasThisPuzzleAnyGood))
                       (d/span {} (trans :pleasVotePuzzle)))
                  (d/p {:className "then"}
                       (d/strong {} (trans :thankYou)))))
         (d/div {:className "box"}
                (when (and auth? (:enabled puzzle))
                  (Vote {:puzzle puzzle
                         :attempt attempt} trans ctrl)))))

(q/defcomponent Right [table trans ctrl]
  (letfn [(center-right [el]
            (set! (-> el .-style .-top) (str (- 256 (/ (.-offsetHeight el) 2)) "px")))]
    (q/wrapper
      (d/div {:className "right"} table)
      :onMount center-right
      :onUpdate center-right)))

(q/defcomponent History [_ url]
  (q/wrapper
    (d/div {:className "history"})
    :onMount (fn [el] (.load ($ el) url))))

(q/defcomponent ViewControls [{{:keys [color game-id initial-ply]} :puzzle fen :fen} urls trans]
  (d/div {:className "game_control"}
         (when game-id
           (d/a {:className "button hint--bottom"
                 :data-hint (trans :fromGameLink game-id)
                 :href (str "/" game-id "/" color "#" initial-ply)}
                (d/span {:data-icon "v"})))
         (d/a {:className "fen_link button hint--bottom"
               :data-hint (trans :boardEditor)
               :href (str (:editor urls) fen)}
              (d/span {:data-icon "m"}))
         (d/a {:className "continue toggle button hint--bottom"
               :data-hint (trans :continueFromHere)
               :onClick #(.toggle ($ :.continue.links))}
              (d/span {:data-icon "U"}))
         (apply d/div {:id "GameButtons"
                       :className "hint--bottom"
                       :data-hint "Review puzzle solution"}
                (map (fn [[id icon]] (d/a {:className (str id " button")
                                           :data-value id
                                           :data-icon icon}))
                     [["first" "W"] ["prev" "Y"] ["next" "X"] ["last" "V"]]))))

(q/defcomponent ContinueLinks [fen trans]
  (d/div {:className "continue links none"}
         (d/a {:className "button"
               :href (str "/?fen=" fen "#ai")} (trans :playWithTheMachine))
         (d/a {:className "button"
               :href (str "/?fen=" fen "#friend")} (trans :playWithAFriend))))

(q/defcomponent Puzzle [{:keys [cg-obj mode puzzle commentary win attempt user difficulty voted]}
                        urls trans ctrl]
  (d/div {:id "puzzle"
          :className "training"}
         (Side {:commentary commentary
                :mode mode
                :win win
                :attempt attempt
                :user user
                :difficulty difficulty} urls trans ctrl)
         (Right (if (= mode "view")
                  (ViewTable {:auth? (boolean user)
                              :attempt attempt
                              :voted voted
                              :puzzle puzzle} trans ctrl)
                  (PlayTable {:turn-color (:turn-color cg-obj)
                              :color (:color puzzle)} trans ctrl)))
         (d/div {:className "center"}
                (chessground.ui/board-component (chessground.ui/clj->react cg-obj ctrl))
                (when (= mode "view")
                  (let [fen (-> cg-obj :chess cg-fen/dump)]
                    (d/div {}
                           (ViewControls {:puzzle puzzle :fen fen} urls trans)
                           (ContinueLinks fen trans))))
                (History {} (:history urls)))))

(defn make-trans [i18n]
  (fn [k & args]
    (reduce (fn [s v] (.replace s "%s" v)) (get i18n k) args)))

(defn root [app ctrl]
  (Puzzle {:mode (:mode app)
           :puzzle (:puzzle app)
           :commentary (:comment app)
           :cg-obj (:chessground app)
           :attempt (:attempt app)
           :win (:win app)
           :voted (:voted app)
           :user (:user app)
           :difficulty (:difficulty app)}
          (:urls app)
          (make-trans (:i18n app))
          ctrl))
