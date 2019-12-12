package lila

package object rating extends PackageObject {

  type UserRankMap = Map[lila.rating.PerfType, Int]

  type RatingFactors = Map[lila.rating.PerfType, RatingFactor]
}
