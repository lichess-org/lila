package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.variant.Variant
import lila.common.paginator.Paginator
import lila.db.BSON._
import lila.db.paginator.BSONAdapter
import lila.db.Types.Coll
import lila.user.User

final class LeaderboardApi(
    coll: Coll,
    maxPerPage: Int) {

  import LeaderboardApi._
  import BSONHandlers._

  def recentByUser(user: User, page: Int) = paginator(user, page, BSONDocument("d" -> -1))

  def bestByUser(user: User, page: Int) = paginator(user, page, BSONDocument("w" -> 1))

  private def paginator(user: User, page: Int, sort: BSONDocument): Fu[Paginator[TourEntry]] = Paginator(
    adapter = new BSONAdapter[Entry](
      collection = coll,
      selector = BSONDocument("u" -> user.id),
      projection = BSONDocument(),
      sort = sort
    ) mapFutureList withTournaments,
    currentPage = page,
    maxPerPage = maxPerPage)

  private def withTournaments(entries: Seq[Entry]): Fu[Seq[TourEntry]] =
    TournamentRepo byIds entries.map(_.tourId) map { tours =>
      entries.flatMap { entry =>
        tours.find(_.id == entry.tourId).map { TourEntry(_, entry) }
      }
    }
}

object LeaderboardApi {

  val rankRatioMultiplier = 100 * 1000

  case class TourEntry(tour: Tournament, entry: Entry)

  case class Entry(
    id: String, // same as tournament player id
    userId: String,
    tourId: String,
    nbGames: Int,
    score: Int,
    rank: Int,
    rankRatio: Int, // ratio * 100000. function of rank and tour.nbPlayers. less is better.
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    date: DateTime)
}
