package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.rating.PerfType
import lila.user.User

final class TournamentShieldApi(
    coll: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import TournamentShield._
  import BSONHandlers._

  def apply(u: User): Fu[List[Owner]] = cache.get map {
    _.filter(_.userId == u.id)
  }

  def apply(pt: PerfType): Fu[Option[Owner]] = cache.get map {
    _.find(_.perfType == pt)
  }

  def apply: Fu[List[Owner]] = cache.get

  private[tournament] def clear = cache.refresh

  private val cache = asyncCache.single[List[Owner]](
    name = "tournament.shield",
    findAll,
    expireAfter = _.ExpireAfterWrite(1 day)
  )

  private def findAll: Fu[List[Owner]] =
    coll.find($doc(
      "schedule.freq" -> scheduleFreqHandler.write(Schedule.Freq.Shield),
      "status" -> statusBSONHandler.write(Status.Finished),
      "startsAt" $gt DateTime.now.minusMonths(1).minusDays(1)
    )).sort($sort desc "startsAt").list[Tournament]() map { tours =>
      PerfType.leaderboardable.flatMap { perfType =>
        tours.flatMap { tour =>
          for {
            tourPerfType <- tour.perfType
            if tourPerfType == perfType
            winner <- tour.winnerId
          } yield Owner(
            perfType = perfType,
            userId = winner,
            since = tour.finishesAt,
            tourId = tour.id
          )
        }.headOption
      }
    }
}

object TournamentShield {

  def spotlight(name: String) = Spotlight(
    iconFont = "5".some,
    headline = s"Battle for the $name Shield",
    description = s"""This Shield trophy is unique.
The winner keeps it for one month,
then must defend it during the next $name tournament!""",
    homepageHours = 6.some
  )

  case class Owner(
      perfType: PerfType,
      userId: User.ID,
      since: DateTime,
      tourId: Tournament.ID
  )
}
