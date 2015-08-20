package lila.tournament

import scala.concurrent.duration.FiniteDuration
import org.joda.time.DateTime

import lila.db.BSON._
import lila.user.{ User, UserRepo }

final class Winners(
    mongoCache: lila.memo.MongoCache.Builder,
    ttl: FiniteDuration) {

  private implicit val WinnerBSONHandler =
    reactivemongo.bson.Macros.handler[Winner]

  private val scheduledCache = mongoCache[Int, List[Winner]](
    prefix = "tournament:winner",
    f = fetchScheduled,
    timeToLive = ttl)

  import Schedule.Freq
  private def fetchScheduled(nb: Int): Fu[List[Winner]] = {
    val since = DateTime.now minusMonths 1
    List(Freq.Marathon, Freq.Monthly, Freq.Weekly, Freq.Daily).map { freq =>
      TournamentRepo.lastFinishedScheduledByFreq(freq, since, 4) flatMap toursToWinners
    }.sequenceFu map (_.flatten) flatMap { winners =>
      TournamentRepo.lastFinishedScheduledByFreq(
        Freq.Hourly, since, math.max(0, nb - winners.size)
      ) flatMap toursToWinners map (winners ::: _)
    }
  }

  private def toursToWinners(tours: List[Tournament]): Fu[List[Winner]] =
    tours.map { tour =>
      PlayerRepo winner tour.id flatMap {
        case Some(player) => UserRepo isEngine player.userId map { engine =>
          !engine option Winner(tour.id, tour.name, player.userId)
        }
        case _ => fuccess(none)
      }
    }.sequenceFu map (_.flatten)

  def scheduled(nb: Int): Fu[List[Winner]] = scheduledCache apply nb
}
