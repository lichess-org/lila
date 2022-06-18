package lila.tutor

import chess.Color
import chess.format.FEN
import chess.opening.FullOpeningDB
import reactivemongo.api.bson._

import lila.common.Iso
import lila.db.dsl._
import lila.db.BSON
import lila.rating.PerfType
import chess.opening.FullOpening

private object TutorBsonHandlers {

  implicit val ratioHandler = doubleAnyValHandler[TutorRatio](_.value, TutorRatio.apply)

  implicit def colorMapHandler[A: BSONHandler]: BSONHandler[Color.Map[A]] =
    implicitly[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  implicit def metricHandler[A: BSONHandler: Ordering]: BSONHandler[TutorMetric[A]] =
    implicitly[BSONHandler[List[A]]]
      .as[TutorMetric[A]](
        list => TutorMetric(list(0), list(1)),
        metric => List(metric.mine, metric.peer)
      )

  trait CanBeUnknown[T] {
    val value: T
    val is = (v: T) => v == value
  }
  implicit val doubleCanBeUnknown = new CanBeUnknown[Double] {
    val value = Double.MinValue
  }

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
  implicit val openingsHandler      = Macros.handler[TutorOpenings]

  // implicit val perfReportHandler = Macros.handler[TutorPerfReport]

  // implicit val perfsHandler: BSONHandler[TutorFullReport.PerfMap] =
  //   implicitly[BSONHandler[Map[String, TutorPerfReport]]].as[TutorFullReport.PerfMap](
  //     _ flatMap { case (key, report) =>
  //       PerfType(key).map(_ -> report)
  //     },
  //     _.mapKeys(_.key)
  //   )
  implicit val fullReportHandler = Macros.handler[TutorFullReport]
}
