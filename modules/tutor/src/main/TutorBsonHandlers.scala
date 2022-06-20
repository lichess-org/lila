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

  implicit val durationHandler: BSONHandler[FiniteDuration] = lila.db.dsl.minutesHandler
  implicit val ratioHandler = doubleAnyValHandler[TutorRatio](_.value, TutorRatio.apply)

  implicit def colorMapHandler[A: BSONHandler]: BSONHandler[Color.Map[A]] =
    implicitly[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  trait CanBeUnknown[T] {
    val value: T
    val is = (v: T) => v == value
  }
  implicit val doubleCanBeUnknown = new CanBeUnknown[Double] {
    val value = Double.MinValue
  }
  implicit val ratioCanBeUnknown = new CanBeUnknown[TutorRatio] {
    val value = TutorRatio(doubleCanBeUnknown.value)
  }

  implicit def metricHandler[A](implicit
      handler: BSONHandler[A],
      unknown: CanBeUnknown[A],
      ordering: Ordering[A]
  ): BSONHandler[TutorMetric[A]] =
    implicitly[BSONHandler[List[A]]]
      .as[TutorMetric[A]](
        list => TutorMetric(list(0), list.lift(1).filterNot(unknown.is)),
        metric => List(metric.mine, metric.peer | unknown.value)
      )

  implicit def metricOptionHandler[A](implicit
      handler: BSONHandler[A],
      unknown: CanBeUnknown[A],
      ordering: Ordering[A]
  ): BSONHandler[TutorMetricOption[A]] =
    implicitly[BSONHandler[List[A]]].as[TutorMetricOption[A]](
      list => TutorMetricOption(list.lift(0).filterNot(unknown.is), list.lift(1).filterNot(unknown.is)),
      metric => List(metric.mine | unknown.value, metric.peer | unknown.value)
    )

  // implicit val timeReportHandler    = Macros.handler[TutorTimeReport]
  implicit val openingFamilyHandler = Macros.handler[TutorOpeningFamily]
  implicit val colorOpeningsHandler = Macros.handler[TutorColorOpenings]
  // implicit val openingsHandler      = Macros.handler[Color.Map[TutorColorOpenings]]

  implicit val phaseHandler = Macros.handler[TutorPhase]
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
  implicit val reportHandler     = Macros.handler[TutorReport]

}
