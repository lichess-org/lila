package lila.tournament

import akka.actor.ActorSystem
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.LilaScheduler
import lila.hub.actorApi.push.TourSoon

final private class TournamentNotify(repo: TournamentRepo, cached: Cached)(implicit
    ec: ExecutionContext,
    scheduler: akka.actor.Scheduler
) {

  private val doneMemo = new lila.memo.ExpireSetMemo(10 minutes)

  LilaScheduler(_.Every(10 seconds), _.AtMost(10 seconds), _.Delay(1 minute)) {
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
