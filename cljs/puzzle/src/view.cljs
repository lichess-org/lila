(ns lichess.puzzle.view
  (:require [lichess.puzzle.core :as core :refer [chess]]
            [jayq.core :as jq :refer [$]]
            [jayq.util :as jqu :refer [log]]
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
         (fn [e] (let [$a ($ (.-target e))
                       $please ($ "#puzzle .please_vote")]
                   (when (.-length $please) (jq/add-class $please :thanks))
                   (jq/add-class $a :active)
                   (jq/xhr [:post (jq/data (jq/parent $a) :post-url)]
                           {:vote (jq/data $a :vote)}
                           #(jq/html $vote %))))))

(defn bind-browse! [$browse]
  (jq/on $browse :click :a #(put! browse-chan (jq/data ($ (.-target %)) :value))))

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

(defn switch-class! [$elem klass switch]
  ((if switch jq/add-class jq/remove-class) $elem klass))

(defn update-fen-links! [fen]
  (doseq [link ($ :a.fen_link $puzzle)]
    (let [href1 (jq/attr ($ link) :href)
          href2 (clojure.string/replace href1 #"fen=[^#]*(#.+)?$" #(str "fen=" fen %2))]
      (jq/attr ($ link) :href href2))))

(defn run! [progress]
  (let [$puzzle ($ :#puzzle)
        $browse ($ :#GameButtons $puzzle)
        $first ($ :.first $browse)
        $prev ($ :.prev $browse)
        $next ($ :.next $browse)
        $last ($ :.last $browse)
        lines (js->clj (jq/data $puzzle :lines))
        line (find-best-line-from-progress lines progress)
        history (vec (make-history (jq/data $puzzle :fen) (conj (seq line) (jq/data $puzzle :move))))
        chessboard (make-chessboard $puzzle)]
    (core/center-right! ($ :.right $puzzle))
    (core/board-marks! $puzzle)
    (core/buttons! $puzzle)
    (core/user-chart! ($ :.user_chart $puzzle))
    (bind-vote! ($ :div.vote_wrap $puzzle))
    (bind-continue! ($ :button.continue $puzzle))
    (bind-browse! $browse)
    (jq/bind ($ :a.continue) :click #(jq/toggle ($ :div.continue)))
    (go
      (loop [step (count progress) animate false]
        (let [[move fen] (get history step)
              is-first (= step 0)
              is-last (= step (- (count history) 1))]
          (switch-class! $first :disabled is-first)
          (switch-class! $prev :disabled is-first)
          (switch-class! $next :disabled is-last)
          (switch-class! $last :disabled is-last)
          (.position chessboard fen animate)
          (.load core/chess fen)
          (update-fen-links! fen)
          (core/color-move! $puzzle move)
          (<! (timeout (+ 50 animation-delay)))
          (let [[browse ch] (alts! [browse-chan continue-chan])]
            (if (= ch continue-chan)
              (play-new! $puzzle)
              (do
                (when (and (= browse "first") (not is-first)) (recur 0 true))
                (when (and (= browse "prev") (not is-first)) (recur (- step 1) true))
                (when (and (= browse "next") (not is-last)) (recur (+ step 1) true))
                (when (and (= browse "last") (not is-last)) (recur (- (count history) 1) true))
                (recur step false)))))))))
