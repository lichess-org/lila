package lila.learn

import lila.common.Form.stringIn

case class StageProgress(scores: Vector[StageProgress.Score]):

  import StageProgress.*

  def withScore(level: Int, s: Score) =
    copy(
      scores = (0 until scores.size.max(level))
        .map: i =>
          scores.lift(i) | Score(0)
        .updated(level - 1, s)
        .toVector
    )

object StageProgress:

  def empty = StageProgress(scores = Vector.empty)

  opaque type Score = Int
  object Score extends OpaqueInt[Score]

  val allStageNames = Set(
    "bishop",
    "capture",
    "castling",
    "check1",
    "check2",
    "checkmate1",
    "combat",
    "enpassant",
    "fork",
    "king",
    "knight",
    "list",
    "outOfCheck",
    "pawn",
    "protection",
    "queen",
    "rook",
    "setup",
    "stalemate",
    "value"
  )

  import play.api.data.Forms.*
  val form = play.api.data.Form:
    mapping(
      "stage" -> stringIn(allStageNames),
      "level" -> number(min = 1, max = 10),
      "score" -> number(min = 0, max = 9000)
    )(Tuple3.apply)(lila.common.extensions.unapply)
