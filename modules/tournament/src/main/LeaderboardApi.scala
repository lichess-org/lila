package lila.tournament

import reactivemongo.api.bson.*
import scalalib.Maths
import scalalib.paginator.{ AdapterLike, Paginator }

import lila.core.chess.Rank
import lila.core.perf.PerfId
import lila.db.dsl.{ *, given }
import lila.rating.PerfType

final class LeaderboardApi(
    repo: LeaderboardRepo,
    tournamentRepo: TournamentRepo
)(using Executor)
    extends lila.core.tournament.leaderboard.Api:

  import LeaderboardApi.*
  import BSONHandlers.given

  private val maxPerPage = MaxPerPage(15)

  def recentByUser(user: User, page: Int) = paginator(user, page, sortBest = false)

  def bestByUser(user: User, page: Int) = paginator(user, page, sortBest = true)

  def timeRange(userId: UserId, range: TimeInterval): Fu[List[Entry]] =
    repo.coll
      .find(
        $doc(
          "u" -> userId,
          "d".$gt(range.start).$lt(range.end)
        )
      )
      .sort($sort.desc("d"))
      .cursor[Entry]()
      .list(100)

  def chart(user: User): Fu[ChartData] =
    repo.coll
      .aggregateList(Int.MaxValue, _.sec): framework =>
        import framework.*
        Match($doc("u" -> user.id)) -> List(
          GroupField("v")("nb" -> SumAll, "points" -> PushField("s"), "ratios" -> PushField("w"))
        )
      .map:
        _.flatMap(leaderboardAggResult.readOpt)
      .map: aggs =>
        ChartData:
          aggs
            .flatMap: agg =>
              PerfType.byId.get(agg._id).map {
                _ -> ChartData.PerfResult(
                  nb = agg.nb,
                  points = ChartData.Ints(agg.points),
                  rank = ChartData.Ints(agg.ratios)
                )
              }
            .sortLike(lila.rating.PerfType.leaderboardable, _._1)

  def getAndDeleteRecent(userId: UserId, since: Instant): Fu[List[TourId]] =
    repo.coll
      .list[Entry](
        $doc(
          "u" -> userId,
          "d".$gt(since)
        )
      )
      .flatMap { entries =>
        (entries.nonEmpty
          .so(repo.coll.delete.one($inIds(entries.map(_.id))).void))
          .inject(entries.map(_.tourId))
      }

  private def paginator(user: User, page: Int, sortBest: Boolean): Fu[Paginator[TourEntry]] =
    Paginator(
      currentPage = page,
      maxPerPage = maxPerPage,
      adapter = new AdapterLike[TourEntry]:
        private val selector   = $doc("u" -> user.id)
        def nbResults: Fu[Int] = repo.coll.countSel(selector)
        def slice(offset: Int, length: Int): Fu[Seq[TourEntry]] =
          repo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> List(
                Sort(if sortBest then Ascending("w") else Descending("d")),
                Skip(offset),
                Limit(length),
                PipelineOperator(
                  $lookup.simple(
                    from = tournamentRepo.coll,
                    as = "tour",
                    local = "t",
                    foreign = "_id"
                  )
                ),
                UnwindField("tour")
              )
            .map: docs =>
              for
                doc   <- docs
                entry <- doc.asOpt[Entry]
                tour  <- doc.getAsOpt[Tournament]("tour")
              yield TourEntry(tour, entry)
    )

object LeaderboardApi:

  import lila.core.tournament.leaderboard.Ratio

  private val rankRatioMultiplier = 100 * 1000

  case class TourEntry(tour: Tournament, entry: Entry)

  case class Entry(
      id: TourPlayerId,
      userId: UserId,
      tourId: TourId,
      nbGames: Int,
      score: Int,
      rank: Rank,
      rankRatio: Ratio, // ratio * rankRatioMultiplier. function of rank and tour.nbPlayers. less is better.
      freq: Option[Schedule.Freq],
      speed: Option[Schedule.Speed],
      perf: PerfType,
      date: Instant
  ) extends lila.core.tournament.leaderboard.Entry

  case class ChartData(perfResults: List[(PerfType, ChartData.PerfResult)]):
    import ChartData.*
    lazy val allPerfResults: PerfResult = perfResults.map(_._2) match
      case head :: tail =>
        tail.foldLeft(head) { case (acc, res) =>
          PerfResult(
            nb = acc.nb + res.nb,
            points = res.points ::: acc.points,
            rank = res.rank ::: acc.rank
          )
        }
      case Nil => PerfResult(0, Ints(Nil), Ints(Nil))

  object ChartData:

    case class Ints(v: List[Int]):
      def mean         = Maths.mean(v)
      def median       = Maths.median(v)
      def sum          = v.sum
      def :::(i: Ints) = Ints(v ::: i.v)

    case class PerfResult(nb: Int, points: Ints, rank: Ints):
      private def rankPercent(n: Double) = (n * 100 / rankRatioMultiplier).toInt
      def rankPercentMean                = rank.mean.map(rankPercent)
      def rankPercentMedian              = rank.median.map(rankPercent)

    case class AggregationResult(_id: PerfId, nb: Int, points: List[Int], ratios: List[Int])
