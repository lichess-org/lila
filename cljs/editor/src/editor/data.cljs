(ns org.lichess.editor.data
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [chessground.data :as cg-data]
            [chessground.common :as cg-common]
            [chessground.api :as cg-api]))

(defn- fen-metadatas [app]
  (let [castles (apply str (map first (filter second (:castles app))))]
    (str (:color app) " "
         (if (empty? castles) "-" castles))))

(defn with-fen [app]
  (let [fen (chessground.fen/dump (get-in app [:chessground :chess]))]
    (assoc app :fen (str fen " " (fen-metadatas app)))))

(defn set-castle [app [id value]]
  (assoc-in app [:castles id] value))

(defn make [config]
  (with-fen
    {:color (:color config)
     :castles (chessground.common/stringify-keys (:castles config))
     :base-url (:baseUrl config)
     :chessground (chessground.api/main
                    {:fen (:fen config)
                     :orientation "white"
                     :movable {:free true
                               :color "both"
                               :dropOff "trash"}
                     :premovable {:enabled false}})}))
