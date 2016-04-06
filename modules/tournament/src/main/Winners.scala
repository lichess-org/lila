package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.duration.FiniteDuration

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
    timeToLive = ttl,
    keyToString = _.toString)

  import Schedule.Freq
  private def fetchScheduled(nb: Int): Fu[List[Winner]] = {
    val since = DateTime.now minusMonths 1
    List(Freq.Monthly, Freq.Weekly, Freq.Daily).map { freq =>
      TournamentRepo.lastFinishedScheduledByFreq(freq, since)
    }.sequenceFu.map(_.flatten) flatMap { stds =>
      TournamentRepo.lastFinishedDaily(chess.variant.Crazyhouse) map (stds ::: _.toList)
    } flatMap toursToWinners
  }

  private def toursToWinners(tours: List[Tournament]): Fu[List[Winner]] =
    tours.sortBy(_.schedule.map(_.freq)).reverse.map { tour =>
      PlayerRepo winner tour.id flatMap {
        case Some(player) => UserRepo isEngine player.userId map { engine =>
          !engine option Winner(tour.id, tour.name, player.userId)
        }
        case _ => fuccess(none)
      }
    }.sequenceFu map (_.flatten take 10)

  def scheduled(nb: Int): Fu[List[Winner]] = scheduledCache apply nb
}
