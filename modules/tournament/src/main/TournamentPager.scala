package lila.tournament

import scala.concurrent.duration._

import org.joda.time.DateTime
import org.joda.time.Minutes

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.i18n.I18nKey
import lila.i18n.{ I18nKeys => trans }
import lila.memo.CacheApi._

final class TournamentPager(
    repo: TournamentRepo,
    cacheApi: lila.memo.CacheApi,
)(implicit
    ec: scala.concurrent.ExecutionContext,
) {

  private val maxPerPage = lila.common.config.MaxPerPage(16)

  import BSONHandlers.tournamentHandler

  def enterable(order: TournamentPager.Order, page: Int) =
    order match {
      case TournamentPager.Order.Hot =>
        Paginator(
          adapter = new AdapterLike[Tournament] {
            def nbResults = Featured.get.map(_.length)
            def slice(offset: Int, length: Int) =
              Featured.get.map(_.slice(offset, offset + length))
          },
          currentPage = page,
          maxPerPage = maxPerPage,
        )
      case _ =>
        Paginator(
          adapter = new Adapter[Tournament](
            collection = repo.coll,
            selector = $doc("status" $lt Status.Finished.id) ++ selector(order),
            projection = none,
            sort = sort(order),
            hint = none,
          ),
          currentPage = page,
          maxPerPage = maxPerPage,
        )
    }

  def finished(order: TournamentPager.Order, page: Int) =
    Paginator(
      adapter = new Adapter[Tournament](
        collection = repo.coll,
        selector = $doc("status" -> Status.Finished.id) ++ selector(order),
        projection = none,
        sort = sort(order),
        hint = none,
      ),
      currentPage = page,
      maxPerPage = maxPerPage,
    )

  private def selector(order: TournamentPager.Order) =
    order match {
      case TournamentPager.Order.Created | TournamentPager.Order.Started =>
        $doc("schedule" $exists false)
      case TournamentPager.Order.Lishogi =>
        $doc("schedule" $exists true)
      case _ => $empty
    }

  private def sort(order: TournamentPager.Order) =
    order match {
      case TournamentPager.Order.Popular => $sort desc "nbPlayers"
      case TournamentPager.Order.Created => $sort desc "createdAt"
      case _                             => $sort desc "startsAt"
    }

  object Featured {
    private val hoursBeforeScore = 24
    private val timeConstant     = 0.15
    private val playerScaler     = 2.0
    private val daysCutoff       = 21

    private val max = 128

    def get = featuredCache.getUnit

    def getHomepage = get.dmap(_.take(6))

    // scala sort is stable
    private def featured =
      repo.enterable.map(_.sortBy(_.startsAt.getSeconds).sortBy(calcScore).take(max))

    private def calcScore(t: Tournament): Int = {
      val minuteDistance = math.abs(Minutes.minutesBetween(DateTime.now, t.startsAt).getMinutes)
      val hourDistance   = minuteDistance / 60.0
      val daysDistance   = hourDistance / 24.0

      // closer to start date, closer to top
      val timeScore = math.exp(timeConstant * (hoursBeforeScore - hourDistance)) atLeast 0
      // popular tours near the top
      val playerScore = (t.nbPlayers > 1) ?? t.nbPlayers * playerScaler
      // far off tours closer to bottom
      val penaltyScore = (daysDistance > daysCutoff) ?? (daysDistance atMost 30)

      -(timeScore + playerScore - penaltyScore).toInt
    }

    private val featuredCache = cacheApi.unit[List[Tournament]] {
      _.refreshAfterWrite(25 minutes)
        .buildAsyncFuture(_ => featured)
    }
  }

}

object TournamentPager {
  sealed abstract class Order(val key: String, val name: I18nKey)

  object Order {
    case object Hot     extends Order("hot", trans.study.hot)
    case object Started extends Order("started", trans.dateStartedNewest)
    case object Created extends Order("created", trans.study.dateAddedNewest)
    case object Popular extends Order("popular", trans.study.mostPopular)
    case object Lishogi extends Order("lishogi", trans.lishogiTournaments)

    val all       = List(Hot, Started, Created, Popular)
    val allButHot = all.filterNot(_ == Hot)
    private val byKey: Map[String, Order] = all.map { o =>
      o.key -> o
    }.toMap
    def apply(key: String, fallback: Order): Order = byKey.getOrElse(key, fallback)
  }
}
