package lila.rating

import alleycats.Zero

import lila.core.perf.{ PerfKey, PerfType }
export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

type UserRankMap   = Map[PerfKey, Int]
type RatingFactors = Map[PerfType, RatingFactor]

given intZero: Zero[IntRating] = Zero(IntRating(0))

val formMapping: play.api.data.Mapping[IntRating] =
  import play.api.data.Forms.number
  import lila.common.Form.into
  number(min = Glicko.minRating.value, max = Glicko.maxRating.value).into[IntRating]
