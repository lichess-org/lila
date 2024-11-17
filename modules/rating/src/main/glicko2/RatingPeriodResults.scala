package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
type RatingPeriodResults[R <: Result] = Map[Rating, List[R]]
