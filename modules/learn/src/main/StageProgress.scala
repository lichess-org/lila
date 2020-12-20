package lila.learn

case class StageProgress(scores: Vector[StageProgress.Score]) extends AnyVal {

  import StageProgress._

  def withScore(level: Int, s: Score) =
    copy(
      scores = (0 until scores.size.max(level))
        .map { i =>
          scores.lift(i) | Score(0)
        }
        .updated(level - 1, s)
        .toVector
    )
}

object StageProgress {

  def empty = StageProgress(scores = Vector.empty)

  case class Score(value: Int) extends AnyVal
}
