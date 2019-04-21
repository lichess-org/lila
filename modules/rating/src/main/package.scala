package lila

package object rating extends PackageObject {

  type UserRankMap = Map[lila.rating.Perf.Key, Int]

  type RatingFactors = Map[lila.rating.PerfType, RatingFactor]
}
