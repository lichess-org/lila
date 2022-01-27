package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerPage
import lila.common.Maths
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.rating.PerfType
import lila.user.User

final class LeaderboardApi(
    repo: LeaderboardRepo,
    tournamentRepo: TournamentRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import LeaderboardApi._
  import BSONHandlers._

  private val maxPerPage = MaxPerPage(15)

  def recentByUser(user: User, page: Int) = paginator(user, page, sortBest = false)

  def bestByUser(user: User, page: Int) = paginator(user, page, sortBest = true)

  def timeRange(userId: User.ID, range: (DateTime, DateTime)): Fu[List[Entry]] =
    repo.coll
      .find(
        $doc(
          "u" -> userId,
          "d" $gt range._1 $lt range._2
        )
      )
      .sort($sort desc "d")
      .cursor[Entry]()
      .list()

  def chart(user: User): Fu[ChartData] = {
    repo.coll
      .aggregateList(
        maxDocs = Int.MaxValue,
        ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match($doc("u" -> user.id)) -> List(
          GroupField("v")("nb" -> SumAll, "points" -> PushField("s"), "ratios" -> PushField("w"))
        )
      }
      .map {
        _ flatMap leaderboardAggregationResultBSONHandler.readOpt
      }
      .map { aggs =>
        ChartData {
          aggs
            .flatMap { agg =>
              PerfType.byId get agg._id map {
                _ -> ChartData.PerfResult(
                  nb = agg.nb,
                  points = ChartData.Ints(agg.points),
                  rank = ChartData.Ints(agg.ratios)
                )
              }
            }
            .sortLike(PerfType.leaderboardable, _._1)
        }
      }
  }

  def getAndDeleteRecent(userId: User.ID, since: DateTime): Fu[List[Tournament.ID]] =
    repo.coll.list[Entry](
      $doc(
        "u" -> userId,
        "d" $gt since
      )
    ) flatMap { entries =>
      (entries.nonEmpty ?? repo.coll.delete.one($inIds(entries.map(_.id))).void) inject entries.map(_.tourId)
    }

  private def paginator(user: User, page: Int, sortBest: Boolean): Fu[Paginator[TourEntry]] =
    Paginator(
      currentPage = page,
      maxPerPage = maxPerPage,
      adapter = new AdapterLike[TourEntry] {
        private val selector   = $doc("u" -> user.id)
        def nbResults: Fu[Int] = repo.coll.countSel(selector)
        def slice(offset: Int, length: Int): Fu[Seq[TourEntry]] =
          repo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(selector) -> List(
                Sort(if (sortBest) Ascending("w") else Descending("d")),
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
            }
            .map { docs =>
              for {
                doc   <- docs
                entry <- doc.asOpt[Entry]
                tour  <- doc.getAsOpt[Tournament]("tour")
              } yield TourEntry(tour, entry)
            }
      }
    )
}

object LeaderboardApi {

  private val rankRatioMultiplier = 100 * 1000

  case class TourEntry(tour: Tournament, entry: Entry)

  case class Ratio(value: Double) extends AnyVal {
    def percent = (value * 100).toInt atLeast 1
  }

  case class Entry(
      id: String, // same as tournament player id
      userId: User.ID,
      tourId: Tournament.ID,
      nbGames: Int,
      score: Int,
      rank: Int,
      rankRatio: Ratio, // ratio * rankRatioMultiplier. function of rank and tour.nbPlayers. less is better.
      freq: Option[Schedule.Freq],
      speed: Option[Schedule.Speed],
      perf: PerfType,
      date: DateTime
  )

  case class ChartData(perfResults: List[(PerfType, ChartData.PerfResult)]) {
    import ChartData._
    lazy val allPerfResults: PerfResult = perfResults.map(_._2) match {
      case head :: tail =>
        tail.foldLeft(head) { case (acc, res) =>
          PerfResult(
            nb = acc.nb + res.nb,
            points = res.points ::: acc.points,
            rank = res.rank ::: acc.rank
          )
        }
      case Nil => PerfResult(0, Ints(Nil), Ints(Nil))
    }
  }

  object ChartData {

    case class Ints(v: List[Int]) {
      def mean         = Maths.mean(v)
      def median       = Maths.median(v)
      def sum          = v.sum
      def :::(i: Ints) = Ints(v ::: i.v)
    }

    case class PerfResult(nb: Int, points: Ints, rank: Ints) {
      private def rankPercent(n: Double) = (n * 100 / rankRatioMultiplier).toInt
      def rankPercentMean                = rank.mean map rankPercent
      def rankPercentMedian              = rank.median map rankPercent
    }

    case class AggregationResult(_id: Int, nb: Int, points: List[Int], ratios: List[Int])
  }
}
