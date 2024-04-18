package lila.report
package ui

import lila.ui.ScalatagsTemplate.{ *, given }

def reportScore(score: Report.Score): Frag =
  span(cls := s"score ${score.color}")(score.value.toInt)
