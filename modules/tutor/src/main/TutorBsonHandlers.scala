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

  implicit val ratioHandler = floatAnyValHandler[TutorRatio](_.value, TutorRatio)

  implicit def colorMapHandler[A: BSONHandler]: BSONHandler[Color.Map[A]] =
    implicitly[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  implicit def metricHandler[A: BSONHandler]: BSONHandler[TutorMetric[A]] =
    implicitly[BSONHandler[List[A]]]
      .as[TutorMetric[A]](
        list => TutorMetric(list(0), list(1)),
        metric => List(metric.mine, metric.field)
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
