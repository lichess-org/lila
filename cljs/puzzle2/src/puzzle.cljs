(ns org.lichess.puzzle
  (:require [chessground.common :refer [pp]]
            [org.lichess.puzzle.data :as data]
            [org.lichess.puzzle.ui :as ui]
            [org.lichess.puzzle.handler :as handler]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :as am]))

(defn- make-router [play-router]
  (fn [action & args]
    (let [actions (.split (name action) ".")
          function (reduce (fn [o a] (aget o a)) (aget play-router "controllers") actions)]
      (.-url (apply function args)))))

(defn- make-trans [i18n]
  (fn [k & args]
    (reduce (fn [s v] (.replace s "%s" v)) (aget i18n (name k)) args)))

(defn ^:export main
  "Application entry point; returns the public JavaScript API"
  [element config play-router i18n]
  (let [chan (a/chan)
        ctrl #(a/put! chan [%1 %2])
        app (-> config
                (js->clj :keywordize-keys true)
                (or {})
                (data/make ctrl (make-router play-router) (make-trans i18n)))
        app-atom (atom app)
        render #(js/React.renderComponent (ui/root %) element)]
    (render app)
    (data/initiate app)
    (am/go-loop
      []
      (let [[k msg] (a/<! chan)]
        (render (swap! app-atom (handler/process k msg)))
        (recur)))
    ))
