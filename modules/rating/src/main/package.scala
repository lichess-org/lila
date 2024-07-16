package lila.rating

import alleycats.Zero

export lila.core.lilaism.Lilaism.{ Perf as _, *, given }
export lila.common.extensions.*

type UserRankMap   = Map[PerfKey, Int]
type RatingFactors = Map[PerfType, RatingFactor]

given intZero: Zero[IntRating] = Zero(IntRating(0))

val formMapping: play.api.data.Mapping[IntRating] =
  import play.api.data.Forms.number
  import lila.common.Form.into
  number(min = Glicko.minRating.value, max = Glicko.maxRating.value).into[IntRating]

val ratingApi: lila.ui.RatingApi = new:
  val toNameKey     = PerfType(_).nameKey
  val toDescKey     = PerfType(_).descKey
  val toIcon        = PerfType(_).icon
  val bestRated     = UserPerfsExt.bestRatedPerf
  val dubiousPuzzle = UserPerfs.dubiousPuzzle
