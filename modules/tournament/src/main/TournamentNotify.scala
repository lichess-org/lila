package lila.tournament

import lila.common.{ Bus, LilaScheduler }
import lila.core.misc.push.TourSoon

final private class TournamentNotify(repo: TournamentRepo, cached: TournamentCache)(using
    Executor,
    Scheduler
):

  private val doneMemo = scalalib.cache.ExpireSetMemo[TourId](10.minutes)

  LilaScheduler("TournamentNotify", _.Every(10.seconds), _.AtMost(10.seconds), _.Delay(1.minute)):
    repo
      .soonStarting(nowInstant.plusMinutes(10), nowInstant.plusMinutes(11), doneMemo.keys)
      .flatMap:
        _.sequentiallyVoid { tour =>
          lila.mon.tournament.notifier.tournaments.increment()
          doneMemo.put(tour.id)
          cached.ranking(tour).map { ranking =>
            if ranking.ranking.nonEmpty then
              Bus
                .publish(
                  TourSoon(
                    tourId = tour.id.value,
                    tourName = tour.name,
                    ranking.ranking.keys,
                    swiss = false
                  ),
                  "tourSoon"
                )
              lila.mon.tournament.notifier.tournaments.increment(ranking.playerIndex.size)
          }
        }
