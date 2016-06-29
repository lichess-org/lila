package lila.learn

import org.joda.time.DateTime

case class LevelProgress(
    level: String,
    score: LevelProgress.Score,
    tries: LevelProgress.Tries,
    updatedAt: DateTime) {

  import LevelProgress._

  def withScore(s: Score) = copy(
    score = Score(score.value max s.value),
    tries = Tries(tries.value + 1),
    updatedAt = DateTime.now)
}

object LevelProgress {

  def empty(level: String) = LevelProgress(
    level = level,
    score = Score(0),
    tries = Tries(0),
    updatedAt = DateTime.now)

  case class Score(value: Int) extends AnyVal
  case class Tries(value: Int) extends AnyVal
}
