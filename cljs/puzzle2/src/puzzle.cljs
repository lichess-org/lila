(ns org.lichess.puzzle
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [org.lichess.puzzle.ui :as ui]
            [org.lichess.puzzle.handler :as handler]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :as am]))

(defn ^:export main
  "Application entry point; returns the public JavaScript API"
  [element config]
  (let [chan (a/chan)
        ctrl #(a/put! chan [%1 %2])
        app (data/make (or (js->clj config :keywordize-keys true) {}))
        app-atom (atom app)
        render #(js/React.renderComponent (ui/root % ctrl) element)]
    (render app)
    (am/go-loop
      []
      (let [[k msg] (a/<! chan)]
        (render (swap! app-atom (handler/process k msg)))
        (recur)))
    ))
