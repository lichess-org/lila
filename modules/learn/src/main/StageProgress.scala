package lila.learn

import org.joda.time.DateTime

case class StageProgress(
  stage: Stage,
  score: StageProgress.Score,
  tries: StageProgress.Tries,
  updatedAt: DateTime)

object StageProgress {

  case class Score(value: Int) extends AnyVal
  case class Tries(value: Int) extends AnyVal
}
