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

  private given [V](using handler: BSONHandler[V]): BSONHandler[Option[ValueCount[V]]] =
    quickHandler[Option[ValueCount[V]]](
      {
        case arr: BSONArray =>
          for v <- arr.getAsOpt[V](0); c <- arr.getAsOpt[Int](1) yield ValueCount(v, c)
        case _ => None
      },
      vcOpt =>
        {
          for vc <- vcOpt; v <- handler.writeOpt(vc.value) yield $arr(v, BSONInteger(vc.count))
        }.getOrElse(BSONNull)
    )

  given [A](using handler: BSONHandler[A], ordering: Ordering[A]): BSONHandler[TutorBothValues[A]] =
    summon[BSONHandler[List[Option[ValueCount[A]]]]]
      .as[TutorBothValues[A]](
        list => TutorBothValues(list(0).get, list.lift(1).flatten),
        metric => List(metric.mine.some, metric.peer)
      )

  given [A](using
      handler: BSONHandler[A],
      ordering: Ordering[A]
  ): BSONHandler[TutorBothValueOptions[A]] =
    summon[BSONHandler[List[Option[ValueCount[A]]]]].as[TutorBothValueOptions[A]](
      list => TutorBothValueOptions(list.lift(0).flatten, list.lift(1).flatten),
      metric => List(metric.mine, metric.peer)
    )

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
