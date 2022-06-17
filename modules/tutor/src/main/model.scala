package lila.tutor

import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.rating.PerfType

case class TutorMetric[A](mine: A, field: A)

case class TutorRatio(value: Float) extends AnyVal
