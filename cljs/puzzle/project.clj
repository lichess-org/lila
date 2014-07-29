(defproject pcg "1.0"
  :description "lichess.org puzzle solver"
  :source-paths ["src-clj"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268" :exclusions [org.apache.ant/ant]]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [jayq "2.5.1"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {
              :builds {
                       :dev {
                             :source-paths ["src"]
                             :compiler {:output-to "../../public/compiled/puzzle.js"
                                        ; :source-map "../../public/compiled/puzzle.js.map"
                                        ; :output-dir "../../public/compiled/target"
                                        :optimizations :whitespace
                                        :pretty-print true}}
                       :prod {
                              :source-paths ["src"]
                              :compiler {:output-to "../../public/compiled/puzzle.js"
                                         :externs ["externs/misc.js" "externs/jquery.js"]
                                         :optimizations :advanced
                                         :pretty-print false}}}})
