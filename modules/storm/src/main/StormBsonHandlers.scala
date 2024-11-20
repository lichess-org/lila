package lila.storm

import chess.format.{ Fen, Uci }
import chess.IntRating
import reactivemongo.api.bson.*

import scala.util.Success

import lila.common.LichessDay
import lila.core.id.PuzzleId
import lila.db.dsl.{ *, given }

object StormBsonHandlers:

  given puzzleReader: BSONDocumentReader[StormPuzzle] with
    def readDocument(r: BSONDocument) = for
      id      <- r.getAsTry[PuzzleId]("_id")
      fen     <- r.getAsTry[Fen.Full]("fen")
      lineStr <- r.getAsTry[String]("line")
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      rating  <- r.getAsTry[IntRating]("rating")
    yield StormPuzzle(id, fen, line, rating)

  given BSONHandler[StormDay.Id] =
    val sep = ':'
    tryHandler[StormDay.Id](
      { case BSONString(v) =>
        v.split(sep) match
          case Array(userId, dayStr) =>
            Success(StormDay.Id(UserId(userId), LichessDay(Integer.parseInt(dayStr))))
          case _ => handlerBadValue(s"Invalid storm day id $v")
      },
      id => BSONString(s"${id.userId}$sep${id.day.value}")
    )

  given BSONDocumentHandler[StormDay] = Macros.handler
