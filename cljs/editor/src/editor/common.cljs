(ns org.lichess.editor.common
  "Shared utilities for the library")

(defn pp [& exprs]
  (doseq [expr exprs] (.log js/console (clj->js expr)))
  (first exprs))
