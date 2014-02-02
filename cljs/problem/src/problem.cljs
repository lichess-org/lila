(ns pcg
  (:require [dommy.core :as dommy])
  (:use-macros
    [dommy.macros :only [node sel sel1]]))

(defn log! [& args] (.log js/console (apply pr-str args)))

; Run the application!
(dommy/listen! (sel1 'document) :click log!)
