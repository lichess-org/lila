package lila.tutor

import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.rating.PerfType

case class TutorMetric[A](mine: A, peer: A)
case class TutorMetricOption[A](mine: Option[A], peer: Option[A])

case class TutorRatio(value: Double) extends AnyVal

object TutorRatio {
  def apply(a: Int, b: Int): TutorRatio = TutorRatio(a.toDouble / b)
}
