package lila.gathering

import chess.Clock
import chess.Clock.{ IncrementSeconds, LimitSeconds }

import lila.common.Form.*

object GatheringClock:

  val times: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ {
    (2 to 8 by 1) ++ (10 to 45 by 5) ++ (50 to 60 by 10)
  }.map(_.toDouble)
  val timeDefault = 2d
  private def formatLimit(l: Double) =
    Clock.Config(LimitSeconds((l * 60).toInt), IncrementSeconds(0)).limitString + {
      if l <= 1 then " minute" else " minutes"
    }
  val timeChoices = optionsDouble(times, formatLimit)

  val increments = IncrementSeconds.from:
    (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val incrementDefault = IncrementSeconds(0)
  val incrementChoices = options(IncrementSeconds.raw(increments), "%d second{s}")
