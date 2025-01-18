package lila.storm

import scala.util.Success

import reactivemongo.api.bson._
import shogi.format.forsyth.Sfen
import shogi.format.usi.UciToUsi
import shogi.format.usi.Usi

import lila.common.Day
import lila.db.dsl._
import lila.puzzle.Puzzle

private object StormBsonHandlers {

  import lila.puzzle.BsonHandlers.PuzzleIdBSONHandler

  implicit val StormPuzzleBSONReader: BSONDocumentReader[StormPuzzle] = new BSONDocumentReader[StormPuzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id]("_id")
      sfen    <- r.getAsTry[Sfen]("sfen")
      lineStr <- r.getAsTry[String]("line")
      line <- lineStr
        .split(' ')
        .toList
        .flatMap(m => Usi.apply(m).orElse(UciToUsi.apply(m)))
        .toNel
        .toTry("Empty move list?!")
      rating <- r.getAsTry[Int]("rating")
    } yield StormPuzzle(id, sfen, line, rating)
  }

  implicit lazy val stormDayIdHandler: BSONHandler[StormDay.Id] = {
    import StormDay.Id
    val sep = ':'
    tryHandler[Id](
      { case BSONString(v) =>
        v split sep match {
          case Array(userId, dayStr) => Success(Id(userId, Day(Integer.parseInt(dayStr))))
          case _                     => handlerBadValue(s"Invalid storm day id $v")
        }
      },
      id => BSONString(s"${id.userId}$sep${id.day.value}")
    )
  }

  implicit val stormDayBSONHandler: BSONDocumentHandler[StormDay] = Macros.handler[StormDay]
}
