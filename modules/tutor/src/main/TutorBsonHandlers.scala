package lila.tutor

import chess.Color
import chess.format.FEN
import chess.opening.FullOpeningDB
import reactivemongo.api.bson._

import lila.common.Iso
import lila.db.dsl._
import lila.rating.PerfType

private object TutorBsonHandlers {

  implicit val fenBSONHandler          = stringAnyValHandler[FEN](_.value, FEN.apply)
  implicit val nbGamesBSONHandler      = intAnyValHandler[NbGames](_.value, NbGames)
  implicit val nbMovesBSONHandler      = intAnyValHandler[NbMoves](_.value, NbMoves)
  implicit val nbMovesRatioBSONHandler = Macros.handler[NbMovesRatio]
  implicit val timeReportBSONHandler   = Macros.handler[TutorTimeReport]

  implicit val colorOpeningsBSONHandler: BSONHandler[TutorOpeningReport.ColorOpeningMap] =
    implicitly[BSONHandler[Map[String, Bdoc]]].as[TutorOpeningReport.ColorOpeningMap](
      pairs =>
        for {
          (fenS, doc) <- pairs
          fen = FEN(fenS)
          opening <- FullOpeningDB.findByFen(fen)
          ply     <- doc.int("ply")
          games   <- doc.getAsOpt[NbGames]("games")
          moves   <- doc.getAsOpt[NbMoves]("moves")
        } yield fen -> TutorOpeningReport(opening, ply, games, moves),
      _ collect {
        case (fen, report) if report.games.value > 0 =>
          fen.value -> $doc(
            "ply"   -> report.ply,
            "games" -> report.games,
            "moves" -> report.moves
          )
      }
    )

  implicit val openingsBSONHandler: BSONHandler[TutorOpeningReport.OpeningMap] =
    implicitly[BSONHandler[Map[String, TutorOpeningReport.ColorOpeningMap]]]
      .as[TutorOpeningReport.OpeningMap](
        doc => Color.Map(~doc.get("w"), ~doc.get("b")),
        map => Map("w" -> map.white, "b" -> map.black)
      )

  implicit val perfReportBSONHandler = Macros.handler[TutorPerfReport]

  implicit val perfsBSONHandler: BSONHandler[TutorFullReport.PerfMap] =
    implicitly[BSONHandler[Map[String, TutorPerfReport]]].as[TutorFullReport.PerfMap](
      _ flatMap { case (key, report) =>
        PerfType(key).map(_ -> report)
      },
      _ collect { case (k, report) if report.nonEmpty => k.key -> report }
    )
  implicit val fullReportBSONHandler = Macros.handler[TutorFullReport]
}
