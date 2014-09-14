(ns org.lichess.puzzle.xhr
  (:require [jayq.core :as jq]))

(defn set-difficulty [state id ctrl]
  (jq/xhr [:post (-> state :urls :difficulty)]
          {:difficulty id}
          #(ctrl :reload %)))

(defn attempt [state win ctrl]
  (jq/xhr [:post (-> state :urls :post)]
          {:win (if win 1 0)
           :time (- (.getTime (js/Date.)) (.getTime (:started-at state)))}
          #(ctrl :reload-with-progress %)))
