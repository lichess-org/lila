package lila.coach

import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.BSON._
import lila.db.Implicits._
import lila.rating.PerfType

private[coach] object BSONHandlers {

  import UserStat.PerfResults
  import Results.{ OutcomeStatuses, StatusScores, BestWin, BestRating, Streak }

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
  implicit val OpeningsBSONHandler = Macros.handler[Openings]
  implicit val ResultsBestWinBSONHandler = Macros.handler[BestWin]
  implicit val ResultsBestRatingBSONHandler = Macros.handler[BestRating]
  implicit val ResultsBSONHandler = Macros.handler[Results]

  private implicit val resultsMapHandler = Map.MapHandler[Results]
  private implicit val PerfResultsBSONHandler = new BSONHandler[BSONDocument, PerfResults] {
    def read(doc: BSONDocument): PerfResults = PerfResults {
      resultsMapHandler read doc mapKeys PerfType.apply collect { case (Some(k), v) => k -> v }
    }
    def write(x: PerfResults) = resultsMapHandler write x.m.mapKeys(_.key)
  }
  implicit val UserStatBSONHandler = Macros.handler[UserStat]
}
