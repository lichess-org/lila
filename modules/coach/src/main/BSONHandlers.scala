package lila.coach

import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.BSON._
import lila.db.Implicits._
import lila.rating.PerfType

private[coach] object BSONHandlers {

  import Results.{ BestWin }
  import PerfResults.{ BestRating, StatusScores, OutcomeStatuses, PerfResultsMap }
  import Openings.OpeningsMap
  import GameSections.Section

  private implicit val intMapHandler = MapValue.MapHandler[Int]

  private implicit val NbSumBSONHandler = new BSONHandler[BSONArray, NbSum] {
    def read(arr: BSONArray) = NbSum(
      nb = arr.getAs[Int](0) err "NbSum missing nb",
      sum = arr.getAs[Int](1) err "NbSum missing sum")
    def write(x: NbSum) = BSONArray(x.nb, x.sum)
  }
  private implicit val StatusScoresBSONHandler = new BSONHandler[BSONDocument, StatusScores] {
    def read(doc: BSONDocument): StatusScores = StatusScores {
      intMapHandler read doc mapKeys { k =>
        parseIntOption(k) flatMap chess.Status.apply
      } collect { case (Some(k), v) => k -> v }
    }
    def write(x: StatusScores) = intMapHandler write x.m.mapKeys(_.id.toString)
  }
  implicit val MoveBSONHandler = Macros.handler[Move]
  implicit val TrimmedMovesBSONHandler = new BSONHandler[BSONArray, TrimmedMoves] {
    def read(a: BSONArray) = TrimmedMoves {
      a.values.collect {
        case BSONInteger(i) => i
      }.grouped(5).foldLeft(Vector.empty[Move]) {
        case (acc, i) =>
          acc :+ Move(i(0), NbSum(i(1), i(2)), NbSum(i(3), i(4)))
      }
    }
    def write(x: TrimmedMoves) = BSONArray {
      x.moves.toStream.flatMap { m =>
        List(m.nb, m.acpl.nb, m.acpl.sum, m.time.nb, m.time.sum)
      }
    }
  }
  implicit val ColorMovesBSONHandler = Macros.handler[ColorMoves]

  implicit val PerfResultsOutcomeStatusesBSONHandler = Macros.handler[OutcomeStatuses]
  implicit val ResultsBestWinBSONHandler = Macros.handler[BestWin]
  implicit val PerfResultsBestRatingBSONHandler = Macros.handler[BestRating]
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
  implicit val ColorResultsBSONHandler = Macros.handler[ColorResults]

  implicit val UserStatBSONHandler = new lila.db.BSON[UserStat] {
    def reads(r: lila.db.BSON.Reader) = {
      UserStat(
        colorResults = r.getO[ColorResults]("colorResults") | ColorResults.empty,
        openings = r.getO[Openings]("openings") | Openings.empty,
        results = r.getO[PerfResults]("results") | PerfResults.empty,
        perfResults = r.getO[PerfResults.PerfResultsMap]("perfResults") | PerfResults.emptyPerfResultsMap
      )
    }
    def writes(w: lila.db.BSON.Writer, o: UserStat) = BSONDocument(
      "colorResults" -> o.colorResults,
      "openings" -> o.openings,
      "results" -> o.results,
      "perfResults" -> o.perfResults)
  }
  implicit val PeriodBSONHandler = Macros.handler[Period]
}
