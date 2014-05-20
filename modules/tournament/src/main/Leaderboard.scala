package lila.tournament

import scala.concurrent.duration.FiniteDuration

import lila.user.{ User, UserRepo }

final class Leaderboard(ttl: FiniteDuration) {

  private val scheduledCache =
    lila.memo.AsyncCache(fetchScheduled, timeToLive = ttl)

  private def fetchScheduled(nb: Int): Fu[List[Winner]] =
    TournamentRepo lastFinishedScheduled nb flatMap { tours =>
      tours.map { tour =>
        (tour.winner.map(_.id) ?? UserRepo.byId) map2 { (user: User) => Winner(tour, user) }
      }.sequence map (_.flatten)
    }

  def scheduled(nb: Int) = scheduledCache apply nb
}
