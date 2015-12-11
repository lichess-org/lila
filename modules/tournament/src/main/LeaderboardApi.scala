package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.variant.Variant
import lila.db.BSON._
import lila.db.Types.Coll
import lila.user.User

final class LeaderboardApi(coll: Coll) {

  import LeaderboardApi._
  import BSONHandlers._

  def recentByUser(user: User, max: Int): Fu[List[TourEntry]] =
    coll.find(BSONDocument("u" -> user.id))
      .sort(BSONDocument("d" -> -1))
      .cursor[Entry]().collect[List](max).flatMap { entries =>
        TournamentRepo byIds entries.map(_.tourId) map { tours =>
          entries.flatMap { entry =>
            tours.find(_.id == entry.tourId).map { TourEntry(_, entry) }
          }
        }
      }
}

object LeaderboardApi {

  case class TourEntry(tour: Tournament, entry: Entry)

  case class Entry(
    id: String, // same as tournament player id
    userId: String,
    tourId: String,
    nbGames: Int,
    score: Int,
    rank: Int,
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    date: DateTime)
}
