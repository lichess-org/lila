(ns org.lichess.editor.common)

(defn pp [& exprs]
  (doseq [expr exprs] (.log js/console (clj->js expr)))
  (first exprs))
