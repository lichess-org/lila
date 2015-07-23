package lila.coach

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

private[coach] final class JSONWriters(
    lightUser: String => Option[lila.common.LightUser]) {

  implicit object JodaDateWrites extends Writes[DateTime] {
    private val isoFormatter = ISODateTimeFormat.dateTime
    def writes(d: DateTime): JsValue = JsString(isoFormatter print d)
  }

  implicit val NbSumWriter = OWrites[NbSum] { s =>
    Json.obj("nb" -> s.nb, "avg" -> s.avg)
  }

  private val AutoEcopeningWriter = Json.writes[Ecopening]
  implicit val EcopeningWriter = OWrites[Ecopening] { o =>
    AutoEcopeningWriter.writes(o) + ("formattedMoves" -> JsString(o.formattedMoves))
  }
  implicit val SectionWriter = Json.writes[GameSections.Section]
  implicit val GameSectionsWriter = Json.writes[GameSections]
  implicit val BestWinWriter = OWrites[Results.BestWin] { o =>
    Json.obj(
      "id" -> o.id,
      "rating" -> o.rating,
      "user" -> lightUser(o.userId).map { u =>
        Json.obj(
          "id" -> u.id,
          "name" -> u.name,
          "title" -> u.title)
      })
  }
  implicit val MoveWriter = Json.writes[Move]
  implicit val TrimmedMovesWriter = Writes[TrimmedMoves] { o =>
    Json.toJson(o.moves)
  }
  implicit val ColorMovesWriter = Json.writes[ColorMoves]
  implicit val ResultsWriter = OWrites[Results] { o =>
    Json.obj(
      "nbGames" -> o.nbGames,
      "nbAnalysis" -> o.nbAnalysis,
      "nbWin" -> o.nbWin,
      "nbLoss" -> o.nbLoss,
      "nbDraw" -> o.nbDraw,
      "ratingDiff" -> o.ratingDiff,
      "gameSections" -> o.gameSections,
      "moves" -> o.moves,
      "bestWin" -> o.bestWin,
      "opponentRatingAvg" -> o.opponentRatingAvg,
      "lastPlayed" -> o.lastPlayed)
  }
  implicit val ColorResultsWriter = Json.writes[ColorResults]
  implicit val OpeningsMapWriter = Writes[Openings.OpeningsMap] { o =>
    Json.obj(
      "map" -> Json.toJson(o.m),
      "results" -> o.results
    )
  }
  implicit val OpeningsWriter = Json.writes[Openings]
  implicit val PerfResultsBestRatingWriter = Json.writes[PerfResults.BestRating]
  implicit val PerfResultsStatusMapWriter = OWrites[Map[chess.Status, Int]] { m =>
    JsObject(m.map { case (status, i) => status.name -> JsNumber(i) })
  }
  implicit val PerfResultsStatusScoresWriter = Json.writes[PerfResults.StatusScores]
  implicit val PerfResultsOutcomeStatusesWriter = Json.writes[PerfResults.OutcomeStatuses]
  implicit val PerfResultsWriter = Json.writes[PerfResults]
  implicit val PerfResultsPerfMapWriter = OWrites[Map[lila.rating.PerfType, PerfResults]] { m =>
    JsObject(m.map { case (pt, res) => pt.key -> PerfResultsWriter.writes(res) })
  }
  implicit val PerfResultsMapWriter = Json.writes[PerfResults.PerfResultsMap]
  implicit val UserStatWriter = Json.writes[UserStat]

  implicit val OpeningWriter: OWrites[chess.Opening] = OWrites { o =>
    Json.obj(
      "code" -> o.code,
      "name" -> o.name,
      "moves" -> o.moves
    )
  }

  implicit val EcopeningFamilyWriter = Json.writes[Ecopening.Family]
}
