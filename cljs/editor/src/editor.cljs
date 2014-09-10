(ns org.lichess.editor
  (:require [org.lichess.editor.common :refer [pp]]
            [org.lichess.editor.data :as data]
            [org.lichess.editor.ui :as ui]
            [chessground.handler :as cg-handler]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :as am]))

(defn ^:export main
  "Application entry point; returns the public JavaScript API"
  [element config]
  (let [chan (a/chan)
        ctrl #(a/put! chan [%1 %2])
        app (data/make config)
        app-atom (atom app)
        render #(js/React.renderComponent (ui/root % ctrl) element)]
    (render app)
    (am/go-loop
      []
      (let [[k msg] (a/<! chan)]
        (render (swap! app-atom (fn [app]
                                  (update-in app [:chessground]
                                             (chessground.handler/process k msg)))))
        (recur)))
    ))
