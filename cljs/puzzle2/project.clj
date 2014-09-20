(defproject org.lichess/puzzle "0.1.0"
  :description "lichess puzzle solver"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [com.facebook/react "0.11.1"]
                 [quiescent "0.1.4"]
                 [jayq "2.5.2"]
                 [org.lichess/chessground "0.5.3"]]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src"]
     :compiler
     {:output-dir "../../public/compiled/puzzle/out"
      :output-to "../../public/compiled/puzzle/puzzle.dev.js"
      :optimizations :none
      :source-map true}}
    :prod
    {:source-paths ["src"]
     :compiler
     {:output-dir "../../public/compiled/puzzle/out-prod"
      :output-to "../../public/compiled/puzzle/puzzle.prod.js"
      :externs ["react/externs/react.js" "externs/jquery.js" "externs/misc.js"]
      :optimizations :advanced
      :pretty-print false
      :output-wrapper true
      :closure-warnings {:externs-validation :off
                         :non-standard-jsdoc :off}}}}})
