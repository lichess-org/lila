package lila.racer

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.socket.Socket.Sri
import lila.puzzle.BsonHandlers.PuzzleIdBSONHandler
import lila.storm.StormPuzzle
import lila.puzzle.Puzzle
import chess.format.FEN
import chess.format.Uci
import lila.rating.Glicko

private object RacerBsonHandlers {

  implicit val playerIdHandler = BSONStringHandler.as[RacerPlayer.Id](
    str =>
      if (str startsWith "@") RacerPlayer.Id.Anon(str drop 1)
      else RacerPlayer.Id.User(str),
    {
      case RacerPlayer.Id.Anon(sessionId) => s"@$sessionId"
      case RacerPlayer.Id.User(id)        => id
    }
  )

  implicit val raceIdHandler     = stringAnyValHandler[RacerRace.Id](_.value, RacerRace.Id.apply)
  implicit val playerBSONHandler = Macros.handler[RacerPlayer]
  implicit val raceBSONHandler   = Macros.handler[RacerRace]

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
