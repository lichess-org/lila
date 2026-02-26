package lila.tutor

import chess.IntRating

import lila.core.perf.UserWithPerfs
import lila.insight.{ ClockPercent, InsightPerfStats, MeanRating }
export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("tutor")

private val supportedPerfs = lila.rating.PerfType.standardWithUltra ::: lila.rating.PerfType.variants

private given Ordering[lila.analyse.AccuracyPercent] = doubleOrdering
private given Ordering[ClockPercent] = doubleOrdering
private given Ordering[IntRating] = intOrdering
private given Ordering[GoodPercent] = doubleOrdering

private given Conversion[UserWithPerfs, User] = _.user

extension (stats: List[InsightPerfStats])
  def totalNbGames = stats.map(_.totalNbGames).sum
  def meanRating = (totalNbGames > 0).option:
    MeanRating(stats.map(s => s.rating.value * s.totalNbGames).sum / totalNbGames)

type Angle = "skills" | "opening" | "time" | "phases" | "pieces"
