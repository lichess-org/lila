(ns org.lichess.editor.handler
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [org.lichess.editor.data :as data]
            [chessground.handler :as cg-handler]))

(defn- do-process [k msg]
  (case k
    :some-stuff           pp
    (fn [app]
      (update-in app [:chessground]
                 (chessground.handler/process k msg)))))

(defn process
  "Return a function that transforms an app data"
  [k msg]
  (fn [app]
    (let [new-app ((do-process k msg) app)]
      (if (contains? new-app :chessground) new-app app))))

