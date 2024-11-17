package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
trait RatingPeriodResults[R <: Result]():
  val results: List[R]
  def getResults(player: Rating): List[R] = results.filter(_.participated(player))

final class BinaryRatingPeriodResults(val results: List[BinaryResult])

final class FloatingRatingPeriodResults(val results: List[Result])
    extends RatingPeriodResults[Result]

final class GameRatingPeriodResults(val results: List[DuelResult]) extends RatingPeriodResults[DuelResult]
