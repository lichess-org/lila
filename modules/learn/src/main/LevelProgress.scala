package lila.learn

import org.joda.time.DateTime

case class StageProgress(
    stage: String,
    score: StageProgress.Score,
    tries: StageProgress.Tries,
    updatedAt: DateTime) {

  import StageProgress._

  def withScore(s: Score) = copy(
    score = Score(score.value max s.value),
    tries = Tries(tries.value + 1),
    updatedAt = DateTime.now)
}

object StageProgress {

  def empty(stage: String) = StageProgress(
    stage = stage,
    score = Score(0),
    tries = Tries(0),
    updatedAt = DateTime.now)

  case class Score(value: Int) extends AnyVal
  case class Tries(value: Int) extends AnyVal
}
