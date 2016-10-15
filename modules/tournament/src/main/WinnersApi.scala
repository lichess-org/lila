package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.duration.FiniteDuration

import chess.variant.Variant
import lila.db.BSON._
import lila.user.{ User, UserRepo }

case class FreqWinners(value: Map[Schedule.Freq, Winner])
case class AllWinners(
  hyperbullet: FreqWinners,
  bullet: FreqWinners,
  superblitz: FreqWinners,
  blitz: FreqWinners,
  classical: FreqWinners,
  variants: Map[Variant, FreqWinners])

final class WinnersApi(
    mongoCache: lila.memo.MongoCache.Builder,
    ttl: FiniteDuration) {

  private implicit val WinnerBSONHandler = reactivemongo.bson.Macros.handler[Winner]

  // private def fetchAll: Fu[AllWinners] =

  // private val allCache = mongoCache.single[AllWinners](
  //   prefix = "tournament:winner:all",
  //   f = fetchAll,
  //   timeToLive = ttl,
  //   keyToString = _.toString)

  // def all: Fu[AllWinners] = allCache(true)

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
    }.sequenceFu.map(_.flatten take 10)

  def scheduled(nb: Int): Fu[List[Winner]] = scheduledCache apply nb
}
