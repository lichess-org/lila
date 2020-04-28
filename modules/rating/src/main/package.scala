package lidraughts

package object rating extends PackageObject {

  type UserRankMap = Map[lidraughts.rating.Perf.Key, Int]

  type RatingFactors = Map[lidraughts.rating.PerfType, RatingFactor]
}
