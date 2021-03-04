package lila.racer

import chess.format.FEN
import chess.format.Uci
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.puzzle.BsonHandlers.PuzzleIdBSONHandler
import lila.puzzle.Puzzle
import lila.rating.Glicko
import lila.storm.StormPuzzle

private object RacerBsonHandlers {

  implicit val StormPuzzleBSONReader = new BSONDocumentReader[StormPuzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id]("_id")
      fen     <- r.getAsTry[FEN]("fen")
      lineStr <- r.getAsTry[String]("line")
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      glicko  <- r.getAsTry[Bdoc]("glicko")
      rating  <- glicko.getAsTry[Double]("r")
    } yield StormPuzzle(id, fen, line, rating.toInt)
  }
}
