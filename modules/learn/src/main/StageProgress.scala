package lila.learn

case class StageProgress(scores: Vector[StageProgress.Score]):

  import StageProgress.*

  def withScore(level: Int, s: Score) =
    copy(
      scores = (0 until scores.size.max(level))
        .map { i =>
          scores.lift(i) | Score(0)
        }
        .updated(level - 1, s)
        .toVector
    )

object StageProgress:

  def empty = StageProgress(scores = Vector.empty)

  case class Score(value: Int) extends AnyVal

  import play.api.data.Forms.*
  val form = play.api.data.Form:
    mapping(
      "stage" -> nonEmptyText,
      "level" -> number,
      "score" -> number
    )(Tuple3.apply)(lila.common.extensions.unapply)
