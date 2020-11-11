package lila.puzzle

import chess.format.{ FEN, Uci }
import reactivemongo.api.bson._

import scala.util.Success

import lila.db.BSON
import lila.db.dsl._
import lila.game.Game
import lila.rating.Glicko

private[puzzle] object BsonHandlers {

  implicit val PuzzleIdBSONHandler = stringIsoHandler(Puzzle.idIso)

  import Puzzle.BSONFields._

  implicit val PuzzleBSONReader = new BSONDocumentReader[Puzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id](id)
      gameId  <- r.getAsTry[Game.ID](gameId)
      fen     <- r.getAsTry[FEN](fen)
      lineStr <- r.getAsTry[String](line)
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      glicko  <- r.getAsTry[Glicko](glicko)
      vote    <- r.getAsTry[Int](vote)
      plays   <- r.getAsTry[Int](plays)
    } yield Puzzle(
      id = id,
      gameId = gameId,
      fen = fen,
      line = line,
      glicko = glicko,
      vote = vote,
      plays = plays
    )
  }

  implicit val RoundIdHandler = tryHandler[Round.Id](
    { case BSONString(v) =>
      v split Round.idSep match {
        case Array(userId, puzzleId) => Success(Round.Id(userId, Puzzle.Id(puzzleId)))
        case _                       => handlerBadValue(s"Invalid puzzle round id $v")
      }
    },
    id => BSONString(id.toString)
  )

  implicit val RoundBSONHandler = Macros.handler[Round]
}
