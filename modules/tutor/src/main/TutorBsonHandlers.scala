package lila.tutor

import chess.Color
import reactivemongo.api.bson.*
import scala.concurrent.duration.FiniteDuration

import lila.common.Iso
import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.insight.{ InsightPerfStats, MeanRating }
import lila.rating.PerfType

private object TutorBsonHandlers:

  import lila.insight.BSONHandlers.given
  import lila.rating.BSONHandlers.perfTypeIdHandler
  import lila.analyse.AnalyseBsonHandlers.given

  given BSONHandler[FiniteDuration] = lila.db.dsl.minutesHandler

  given [A](using handler: BSONHandler[A]): BSONHandler[Color.Map[A]] =
    summon[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  private given [V](using handler: BSONHandler[V]): BSONHandler[Option[ValueCount[V]]] =
    quickHandler[Option[ValueCount[V]]](
      {
        case arr: BSONArray =>
          for { v <- arr.getAsOpt[V](0); c <- arr.getAsOpt[Int](1) } yield ValueCount(v, c)
        case _ => None
      },
      vcOpt =>
        {
          for { vc <- vcOpt; v <- handler.writeOpt(vc.value) } yield $arr(v, BSONInteger(vc.count))
        } getOrElse BSONNull
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

  // given BSONDocumentHandler[TutorTimeReport] = Macros.handler
  given BSONDocumentHandler[TutorOpeningFamily] = Macros.handler
  given BSONDocumentHandler[TutorColorOpenings] = Macros.handler
  // given BSONDocumentHandler[Color.Map[TutorColorOpenings]] = Macros.handler

  given BSONDocumentHandler[TutorPhase] = Macros.handler

  given BSONDocumentHandler[TutorFlagging] = Macros.handler
  // given BSONDocumentHandler[TutorPhases] = Macros.handler

  // given BSONDocumentHandler[TutorPerfReport] = Macros.handler

  // implicit val perfsHandler: BSONHandler[TutorFullReport.PerfMap] =
  //   implicitly[BSONHandler[Map[String, TutorPerfReport]]].as[TutorFullReport.PerfMap](
  //     _ flatMap { case (key, report) =>
  //       PerfType(key).map(_ -> report)
  //     },
  //     _.mapKeys(_.key)
  //   )
  given BSONHandler[MeanRating]               = intAnyValHandler(_.value, MeanRating.apply)
  given BSONDocumentHandler[InsightPerfStats] = Macros.handler
  given BSONDocumentHandler[TutorPerfReport]  = Macros.handler
  given BSONDocumentHandler[TutorFullReport]  = Macros.handler
