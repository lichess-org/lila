package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
trait RatingPeriodResults[R <: Result]():
  val results: List[R]
  def getResults(player: Rating): List[R] = results.filter(_.participated(player))
  def getParticipants: Set[Rating]        = results.flatMap(_.players).toSet

class GameRatingPeriodResults(val results: List[GameResult]) extends RatingPeriodResults[GameResult]

class FloatingRatingPeriodResults(val results: List[FloatingResult])
    extends RatingPeriodResults[FloatingResult]
