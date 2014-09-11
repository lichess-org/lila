(defproject org.lichess/editor "0.1.0"
  :description "lichess board editor"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [com.facebook/react "0.11.1"]
                 [org.lichess/chessground "0.3.4"]]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src"]
     :compiler
     {:output-dir "../../public/compiled/editor/out"
      :output-to "../../public/compiled/editor/editor.dev.js"
      :optimizations :none
      :source-map true}}
    :prod
    {:source-paths ["src"]
     :compiler
     {:output-dir "../../public/compiled/editor/out-prod"
      :output-to "../../public/compiled/editor/editor.prod.js"
      :externs ["libs/interact.js" "react/externs/react.js"]
      :optimizations :advanced
      :pretty-print false
      :output-wrapper true
      :closure-warnings {:externs-validation :off
                         :non-standard-jsdoc :off}}}}})
