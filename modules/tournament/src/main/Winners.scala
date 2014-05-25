package lila.tournament

import scala.concurrent.duration.FiniteDuration

import lila.user.{ User, UserRepo }

final class Winners(ttl: FiniteDuration) {

  private val scheduledCache =
    lila.memo.AsyncCache(fetchScheduled, timeToLive = ttl)

  import Schedule.Freq
  private def fetchScheduled(nb: Int): Fu[List[Winner]] =
    List(Freq.Monthly, Freq.Weekly, Freq.Daily).map { freq =>
      TournamentRepo.lastFinishedScheduledByFreq(freq, 3) map toursToWinners
    }.sequenceFu map (_.flatten) flatMap { winners =>
      TournamentRepo.lastFinishedScheduledByFreq(
        Freq.Hourly, math.max(0, nb - winners.size)
      ) map toursToWinners map (winners ::: _)
    }

  private def toursToWinners(tours: List[Finished]) =
    tours.flatMap { tour => tour.winner map { w => Winner(tour.id, tour.name, w.id) } }

  def scheduled(nb: Int) = scheduledCache apply nb
}
