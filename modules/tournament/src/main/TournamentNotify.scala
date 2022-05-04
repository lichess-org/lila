package lila.tournament

import akka.actor.ActorSystem
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.{ AtMost, Every, ResilientScheduler }
import lila.hub.actorApi.push.TourSoon

final private class TournamentNotify(repo: TournamentRepo, cached: Cached)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) {

  private val doneMemo = new lila.memo.ExpireSetMemo(10 minutes)

  ResilientScheduler(every = Every(10 seconds), timeout = AtMost(10 seconds), initialDelay = 1 minute) {
    repo
      .soonStarting(DateTime.now.plusMinutes(10), DateTime.now.plusMinutes(11), doneMemo.keys)
      .flatMap {
        _.map { tour =>
          lila.mon.tournament.notifier.tournaments.increment()
          doneMemo put tour.id
          cached ranking tour map { ranking =>
            if (ranking.ranking.nonEmpty) {
              Bus
                .publish(
                  TourSoon(
                    tourId = tour.id,
                    tourName = tour.name,
                    ranking.ranking.keys,
                    swiss = false
                  ),
                  "tourSoon"
                )
              lila.mon.tournament.notifier.tournaments.increment(ranking.playerIndex.size)
            }
          }
        }.sequenceFu.void
      }
  }
}
