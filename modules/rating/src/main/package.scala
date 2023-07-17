package lila.rating

import alleycats.Zero

export lila.Lila.{ *, given }

type UserRankMap   = Map[lila.rating.PerfType, Int]
type RatingFactors = Map[lila.rating.PerfType, RatingFactor]

given intZero: Zero[IntRating] = Zero(IntRating(0))

val formMapping: play.api.data.Mapping[IntRating] =
  import play.api.data.Forms.number
  import lila.common.Form.into
  number(min = Glicko.minRating.value, max = Glicko.maxRating.value).into[IntRating]
