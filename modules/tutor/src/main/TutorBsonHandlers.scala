package lila.tutor

import reactivemongo.api.bson._

import lila.common.Iso
import lila.db.dsl._
import lila.rating.PerfType

private object TutorBsonHandlers {

  implicit val nbGamesBSONHandler      = intAnyValHandler[NbGames](_.value, NbGames)
  implicit val nbMovesBSONHandler      = intAnyValHandler[NbMoves](_.value, NbMoves)
  implicit val nbMovesRatioBSONHandler = Macros.handler[NbMovesRatio]
  implicit val timeReportBSONHandler   = Macros.handler[TutorTimeReport]

  implicit val perfsBSONHandler: BSONHandler[TutorFullReport.PerfMap] =
    implicitly[BSONHandler[Map[String, TutorTimeReport]]].as[TutorFullReport.PerfMap](
      _ flatMap { case (key, report) =>
        PerfType(key).map(_ -> report)
      },
      _ collect { case (k, report) if report.games.value > 0 => k.key -> report }
    )
  implicit val fullReportBSONHandler = Macros.handler[TutorFullReport]
}
