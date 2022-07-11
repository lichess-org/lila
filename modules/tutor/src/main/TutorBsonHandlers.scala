package lila.tutor

import chess.Color
import reactivemongo.api.bson._
import scala.concurrent.duration.FiniteDuration

import lila.common.Iso
import lila.db.BSON
import lila.db.dsl._
import lila.insight.{ InsightPerfStats, MeanRating }
import lila.rating.PerfType

private object TutorBsonHandlers {

  import lila.insight.BSONHandlers._
  import lila.rating.BSONHandlers.perfTypeIdHandler
  import lila.analyse.AnalyseBsonHandlers.accuracyPercentHandler

  implicit val ratingHandler   = doubleAsIntHandler[Rating](_.value, Rating.apply, 100)
  implicit val durationHandler = lila.db.dsl.minutesHandler
  implicit val ratioHandler    = percentAsIntHandler[GoodPercent](_.value, GoodPercent.apply)

  implicit def colorMapHandler[A: BSONHandler]: BSONHandler[Color.Map[A]] =
    implicitly[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  implicit private def valueCountHandler[V](implicit handler: BSONHandler[V]) =
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

  implicit def metricHandler[A](implicit
      handler: BSONHandler[A],
      ordering: Ordering[A]
  ): BSONHandler[TutorBothValues[A]] =
    implicitly[BSONHandler[List[Option[ValueCount[A]]]]]
      .as[TutorBothValues[A]](
        list => TutorBothValues(list(0).get, list.lift(1).flatten),
        metric => List(metric.mine.some, metric.peer)
      )

  implicit def metricOptionHandler[A](implicit
      handler: BSONHandler[A],
      ordering: Ordering[A]
  ): BSONHandler[TutorBothValueOptions[A]] =
    implicitly[BSONHandler[List[Option[ValueCount[A]]]]].as[TutorBothValueOptions[A]](
      list => TutorBothValueOptions(list.lift(0).flatten, list.lift(1).flatten),
      metric => List(metric.mine, metric.peer)
    )

  // implicit val timeReportHandler    = Macros.handler[TutorTimeReport]
  implicit val openingFamilyHandler = Macros.handler[TutorOpeningFamily]
  implicit val colorOpeningsHandler = Macros.handler[TutorColorOpenings]
  // implicit val openingsHandler      = Macros.handler[Color.Map[TutorColorOpenings]]

  implicit val phaseHandler = Macros.handler[TutorPhase]

  implicit val flaggingHandler = Macros.handler[TutorFlagging]
  // implicit val phasesHandler = Macros.handler[TutorPhases]

  // implicit val perfReportHandler = Macros.handler[TutorPerfReport]

  // implicit val perfsHandler: BSONHandler[TutorFullReport.PerfMap] =
  //   implicitly[BSONHandler[Map[String, TutorPerfReport]]].as[TutorFullReport.PerfMap](
  //     _ flatMap { case (key, report) =>
  //       PerfType(key).map(_ -> report)
  //     },
  //     _.mapKeys(_.key)
  //   )
  implicit val meanRatingHandler = intAnyValHandler[MeanRating](_.value, MeanRating.apply)
  implicit val perfStatsHandler  = Macros.handler[InsightPerfStats]
  implicit val perfReportHandler = Macros.handler[TutorPerfReport]
  implicit val reportHandler     = Macros.handler[TutorFullReport]

}
