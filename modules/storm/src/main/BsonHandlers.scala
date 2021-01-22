package lila.storm

import chess.format.{ FEN, Uci }
import reactivemongo.api.bson._

import lila.db.BSON
import lila.db.dsl._
import lila.puzzle.Puzzle

private[storm] object BsonHandlers {

  import lila.puzzle.BsonHandlers.{ PuzzleIdBSONHandler }

  implicit val StormPuzzleBSONReader = new BSONDocumentReader[StormPuzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id]("_id")
      fen     <- r.getAsTry[FEN]("fen")
      lineStr <- r.getAsTry[String]("line")
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
    } yield StormPuzzle(id, fen, line)
  }
}
