package lila.puzzle

import cats.data.NonEmptyList
import chess.format.{ FEN, Forsyth, Uci }

import lila.rating.Glicko

case class Puzzle(
    id: Puzzle.Id,
    gameId: lila.game.Game.ID,
    fen: FEN,
    line: NonEmptyList[Uci.Move],
    glicko: Glicko,
    vote: Int,
    plays: Int
) {

  def color = fen.color | chess.White

  // ply after "initial move" when we start solving
  def initialPly: Int =
    fen.fullMove ?? { fm =>
      fm * 2 - color.fold(0, 1)
    }

  def fenAfterInitialMove: FEN = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(line.head).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
  } err s"Can't apply puzzle $id first move"
}

object Puzzle {

  case class Id(value: String) extends AnyVal with StringValue

  case class PathId(value: String) extends AnyVal with StringValue

  case class UserResult(
      puzzleId: Id,
      userId: lila.user.User.ID,
      result: Result,
      rating: (Int, Int)
  )

  object BSONFields {
    val id     = "_id"
    val gameId = "gameId"
    val fen    = "fen"
    val line   = "line"
    val glicko = "glicko"
    val vote   = "vote"
    val day    = "day"
    val plays  = "plays"
  }

  implicit val idIso     = lila.common.Iso.string[Id](Id.apply, _.value)
  implicit val pathIdIso = lila.common.Iso.string[PathId](PathId.apply, _.value)
}
