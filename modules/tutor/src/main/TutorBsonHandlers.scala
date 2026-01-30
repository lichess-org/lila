package lila.tutor

import chess.ByColor
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.insight.InsightPerfStats
import scala.util.Try

private object TutorBsonHandlers:

  // *export* insight handlers to ensure NoDbHandler[WinPercent]
  export lila.insight.BSONHandlers.given
  import lila.rating.BSONHandlers.perfTypeIdHandler

  given BSONHandler[FiniteDuration] = lila.db.dsl.minutesHandler
  given BSONHandler[GoodPercent] = percentAsIntHandler[GoodPercent]

  given [A](using handler: BSONHandler[A]): BSONHandler[ByColor[A]] =
    mapHandler[A]
      .as[ByColor[A]](
        doc => ByColor(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  given [A](using handler: BSONHandler[A], ordering: Ordering[A]): BSONHandler[TutorBothOption[A]] =
    quickHandler[TutorBothOption[A]](
      {
        case arr: BSONArray =>
          for
            v <- arr.getAsOpt[A](0)
            c <- arr.getAsOpt[Int](1)
            p <- arr.getAsOpt[A](2)
          yield TutorBothValues(ValueCount(v, c), p)
        case _ => None
      },
      bothOpt =>
        {
          for
            b <- bothOpt
            v <- handler.writeOpt(b.mine.value)
            p <- handler.writeOpt(b.peer)
          yield $arr(v, BSONInteger(b.mine.count), p)
        }.getOrElse(BSONNull)
    )
  given [A](using BSONHandler[A], Ordering[A]): BSONHandler[TutorBothValues[A]] =
    summon[BSONHandler[TutorBothOption[A]]].as(_.get, Some(_))

  given BSONDocumentHandler[TutorOpeningFamily] = Macros.handler

  // survive to an opening family that has since disappeared
  given BSONDocumentHandler[TutorColorOpenings] = new:
    val writer = Macros.writer[TutorColorOpenings]
    export writer.writeTry
    def readDocument(doc: BSONDocument): Try[TutorColorOpenings] =
      doc
        .getAsTry[List[Bdoc]]("families")
        .map: docs =>
          TutorColorOpenings(docs.flatMap(_.asOpt[TutorOpeningFamily]))

  given BSONDocumentHandler[TutorPhase] = Macros.handler
  given BSONDocumentHandler[TutorFlagging] = Macros.handler
  given BSONDocumentHandler[InsightPerfStats] = Macros.handler
  given BSONDocumentHandler[TutorPerfReport] = Macros.handler
  given BSONDocumentHandler[TutorFullReport] = Macros.handler
