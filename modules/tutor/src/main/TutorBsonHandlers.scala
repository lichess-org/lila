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

  implicit val fenBSONHandler          = stringAnyValHandler[FEN](_.value, FEN.apply)
  implicit val nbGamesBSONHandler      = intAnyValHandler[NbGames](_.value, NbGames)
  implicit val nbMovesBSONHandler      = intAnyValHandler[NbMoves](_.value, NbMoves)
  implicit val nbMovesRatioBSONHandler = Macros.handler[NbMovesRatio]
  implicit val timeReportBSONHandler   = Macros.handler[TutorTimeReport]
  implicit val openingBSONHandler = tryHandler[FullOpening](
    { case BSONString(fen) => FullOpeningDB findByFen FEN(fen) toTry s"No such opening: $fen" },
    o => BSONString(o.fen)
  )
  implicit val openingReportBSONHandler = Macros.handler[TutorOpeningReport]

  implicit def colorMapBSONHandler[A: BSONHandler]: BSONHandler[Color.Map[A]] =
    implicitly[BSONHandler[Map[String, A]]]
      .as[Color.Map[A]](
        doc => Color.Map(doc("w"), doc("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  implicit val perfReportBSONHandler = Macros.handler[TutorPerfReport]

  implicit val perfsBSONHandler: BSONHandler[TutorFullReport.PerfMap] =
    implicitly[BSONHandler[Map[String, TutorPerfReport]]].as[TutorFullReport.PerfMap](
      _ flatMap { case (key, report) =>
        PerfType(key).map(_ -> report)
      },
      _.mapKeys(_.key)
    )
  implicit val fullReportBSONHandler = Macros.handler[TutorFullReport]
}
