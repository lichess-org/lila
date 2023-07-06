package lila.racer

import chess.format.Fen
import chess.format.Uci
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.storm.StormPuzzle

private object RacerBsonHandlers:

  given BSONDocumentReader[StormPuzzle] = r =>
    for
      id      <- r.getAsTry[PuzzleId]("_id")
      fen     <- r.getAsTry[Fen.Epd]("fen")
      lineStr <- r.getAsTry[String]("line")
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      glicko  <- r.getAsTry[Bdoc]("glicko")
      rating  <- glicko.getAsTry[Double]("r")
    yield StormPuzzle(id, fen, line, IntRating(rating.toInt))
