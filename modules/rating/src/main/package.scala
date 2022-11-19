package lila.rating

export lila.Lila.{ *, given }

type UserRankMap   = Map[lila.rating.PerfType, Int]
type RatingFactors = Map[lila.rating.PerfType, RatingFactor]
