(ns org.lichess.editor.ui
  (:require [org.lichess.editor.common :as common :refer [pp]]
            [chessground.ui :as cg-ui]))

(def ^:private dom (.-DOM js/React))
(def ^:private div (.-div dom))
(def ^:private p (.-p dom))
(def ^:private a (.-a dom))
(defn- str-replace [in pattern replacement flags]
  (.replace in (js/RegExp. pattern flags) replacement))

(defn- make-url [base fen]
  (str base
       (-> fen
           js/encodeURIComponent
           (str-replace "%20" "_" "g")
           (str-replace "%2F" "/" "g"))))

(def fen-input-component
  (js/React.createClass
    #js
    {:displayName "Editor"
     :render
     (fn []
       (this-as
         this
         (p #{}
            ((.-strong dom) #js {:className "name"} "FEN")
            ((.-input dom) #js {:className "copyable fen-string"
                                :readOnly true
                                :spellcheck false
                                :value (aget (.-props this) "fen")}))))}))

(def url-input-component
  (js/React.createClass
    #js
    {:displayName "Editor"
     :render
     (fn []
       (this-as
         this
         (let [props (.-props this)
               base-url (aget props "base-url")
               fen (aget props "fen")
               url (make-url base-url fen)]
           (p #{}
              ((.-strong dom) #js {:className "name"} "URL")
              ((.-input dom) #js {:className "copyable permalink"
                                  :readOnly true
                                  :spellcheck false
                                  :value url})))))}))

(def controls-component
  (js/React.createClass
    #js
    {:displayName "Editor"
     :render
     (fn []
       (this-as
         this
         (let [props (.-props this)
               ctrl (aget props "ctrl")
               color (aget props "color")
               castles (aget props "castles")
               fen (aget props "fen")
               base-url (aget props "base-url")
               castle-checkbox
               (fn [label id reversed]
                 (let [input ((.-input dom) #js {:id (str "castlink-" id)
                                                 :type "checkbox"
                                                 :defaultChecked (aget castles id)
                                                 :onChange #(ctrl :set-castle [id (-> % .-target .-checked)])})]
                   (if reversed
                     ((.-label dom) #js {} input label)
                     ((.-label dom) #js {} label input))))]
           (div #js {:id "editor-side"}
                (div #js {}
                     (a #js {:className "button"
                             :onClick #(ctrl :start nil)}
                        "Start position")
                     (a #js {:className "button"
                             :onClick #(ctrl :clear nil)}
                        "Clear board"))
                (div #js {}
                     (a #js {:className "button"
                             :data-icon "B"
                             :onClick #(ctrl :toggle-orientation nil)}
                        "Flip board")
                     (a #js {:className "button"
                             :onClick (fn []
                                        (let [fen (js/prompt "Paste FEN position")]
                                          (when (not= "" (.trim fen))
                                            (set! js/window.location (make-url base-url fen)))))}
                        "Load position"))
                (div #js {}
                     ((.-select dom) #js {:className "color"
                                          :defaultValue color
                                          :onChange #(ctrl :set-color (-> % .-target .-value))}
                      ((.-option dom) #js {:value "w"} "White plays")
                      ((.-option dom) #js {:value "b"} "Black plays")))
                (div #js {:className "castling"}
                     ((.-strong dom) #js {} "Castling")
                     (div #js {}
                          (castle-checkbox "White O-O" "K" false)
                          (castle-checkbox "White O-O-O" "Q" true))
                     (div #js {}
                          (castle-checkbox "Black O-O" "k" false)
                          (castle-checkbox "Black O-O-O" "k" true)))
                (div #js {}
                     (a #js {:className "button"
                             :href (str "/?fen=" fen "#ai")}
                        "Play with the machine")
                     (a #js {:className "button"
                             :href (str "/?fen=" fen "#friend")}
                        "Play with a friend"))))))}))


(def editor-component
  (js/React.createClass
    #js
    {:displayName "Editor"
     :render
     (fn []
       (this-as
         this
         (let [props (.-props this)
               ctrl (aget props "ctrl")
               fen (aget props "fen")
               base-url (aget props "base-url")]
           (div #js {:className "editor"} ; won't work without the class name D:
                (chessground.ui/board-component (aget props "chessground"))
                (controls-component #js {:ctrl ctrl
                                         :fen fen
                                         :color (aget props "color")
                                         :castles (aget props "castles")
                                         :base-url base-url})
                (div #js {:className "copyables"}
                     (fen-input-component #js {:fen fen} )
                     (url-input-component #js {:base-url base-url
                                               :fen fen} ))))))}))

(defn root [app ctrl]
  (editor-component
    #js {:fen (:fen app)
         :color (:color app)
         :castles (clj->js (:castles app))
         :base-url (:base-url app)
         :ctrl ctrl
         :chessground (chessground.ui/clj->react (:chessground app) ctrl)}))
