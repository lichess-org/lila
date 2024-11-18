package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
type RatingPeriodResults[R <: Result] = Map[Rating, List[R]]

object RatingPeriodResults:
  def apply[R <: Result](a: (Rating, List[R])) = Map[Rating, List[R]](a)
  def apply[R <: Result](a: (Rating, List[R]), b: (Rating, List[R])) = Map[Rating, List[R]](a, b)
