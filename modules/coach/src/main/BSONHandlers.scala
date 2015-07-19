package lila.coach

import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.BSON._
import lila.db.Implicits._
import lila.rating.PerfType

private[coach] object BSONHandlers {

  import Results.{ BestWin, BestRating, Streak }
  import PerfResults.{ StatusScores, OutcomeStatuses, PerfResultsMap }
  import Openings.OpeningsMap
  import GameSections.Section

  private implicit val intMapHandler = MapValue.MapHandler[Int]

  private implicit val StatusScoresBSONHandler = new BSONHandler[BSONDocument, StatusScores] {
    def read(doc: BSONDocument): StatusScores = StatusScores {
      intMapHandler read doc mapKeys { k =>
        parseIntOption(k) flatMap chess.Status.apply
      } collect { case (Some(k), v) => k -> v }
    }
    def write(x: StatusScores) = intMapHandler write x.m.mapKeys(_.id.toString)
  }
  implicit val ResultsStreakBSONHandler = Macros.handler[Streak]
  implicit val ResultsOutcomeStatusesBSONHandler = Macros.handler[OutcomeStatuses]
  implicit val ResultsBestWinBSONHandler = Macros.handler[BestWin]
  implicit val ResultsBestRatingBSONHandler = Macros.handler[BestRating]
  implicit val SectionBSONHandler = Macros.handler[Section]
  implicit val GameSectionsBSONHandler = Macros.handler[GameSections]
  implicit val ResultsBSONHandler = Macros.handler[Results]
  implicit val PerfResultsBSONHandler = Macros.handler[PerfResults]

  private val perfResultsMapHandler = Map.MapHandler[PerfResults]
  private implicit val PerfResultsMapBSONHandler = new BSONHandler[BSONDocument, PerfResultsMap] {
    def read(doc: BSONDocument): PerfResultsMap = PerfResultsMap {
      perfResultsMapHandler read doc mapKeys PerfType.apply collect { case (Some(k), v) => k -> v }
    }
    def write(x: PerfResultsMap) = perfResultsMapHandler write x.m.mapKeys(_.key)
  }

  private val resultsMapHandler = Map.MapHandler[Results]
  private implicit val OpeningsMapBSONHandler = new BSONHandler[BSONDocument, OpeningsMap] {
    def read(doc: BSONDocument): OpeningsMap = OpeningsMap(resultsMapHandler read doc)
    def write(x: OpeningsMap) = resultsMapHandler write x.m
  }
  implicit val OpeningsBSONHandler = Macros.handler[Openings]

  implicit val UserStatBSONHandler = Macros.handler[UserStat]
}
