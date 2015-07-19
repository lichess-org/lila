package lila.coach

import play.api.libs.json._

final class JsonView {

  def apply(userStat: UserStat): Fu[JsObject] = fuccess {
    UserStatWriter writes userStat
  }

  private implicit val SectionWriter = Json.writes[GameSections.Section]
  private implicit val GameSectionsWriter = Json.writes[GameSections]
  private implicit val BestWinWriter = Json.writes[Results.BestWin]
  private implicit val ResultsWriter = Json.writes[Results]
  private implicit val ColorResultsWriter = Json.writes[ColorResults]
  private implicit val OpeningsMapWriter = Json.writes[Openings.OpeningsMap]
  private implicit val OpeningsWriter = Json.writes[Openings]
  private implicit val PerfResultsBestRatingWriter = Json.writes[PerfResults.BestRating]
  private implicit val PerfResultsStatusMapWriter = OWrites[Map[chess.Status, Int]] { m =>
    JsObject(m.map { case (status, i) => status.id.toString -> JsNumber(i) })
  }
  private implicit val PerfResultsStreakWriter = Json.writes[PerfResults.Streak]
  private implicit val PerfResultsStatusScoresWriter = Json.writes[PerfResults.StatusScores]
  private implicit val PerfResultsOutcomeStatusesWriter = Json.writes[PerfResults.OutcomeStatuses]
  private implicit val PerfResultsWriter = Json.writes[PerfResults]
  private implicit val PerfResultsPerfMapWriter = OWrites[Map[lila.rating.PerfType, PerfResults]] { m =>
    JsObject(m.map { case (pt, res) => pt.key -> PerfResultsWriter.writes(res) })
  }
  private implicit val PerfResultsMapWriter = Json.writes[PerfResults.PerfResultsMap]
  private implicit val UserStatWriter = Json.writes[UserStat]
}
