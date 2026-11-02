package lila
package tournament

import com.github.blemale.scaffeine.AsyncLoadingCache
import scalalib.model.Minutes

import lila.memo.CacheApi.buildAsyncTimeout
import lila.mon.extensions.*

final class TournamentFeaturing(
    api: TournamentApi,
    repo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  object tourIndex:
    def get(teamIds: List[TeamId]): Fu[(List[Tournament], VisibleTournaments)] = for
      (base, scheduled) <- sameForEveryone.get(())
      teamTours <- visibleForTeams(teamIds, Minutes(5 * 60), "index", Max(30))
      forMe = base.add(teamTours)
    yield (scheduled, forMe)

    private val sameForEveryone =
      cacheApi.unit[(VisibleTournaments, List[Tournament])]("tournamentFeaturing.index.sameForEveryone"):
        _.refreshAfterWrite(3.seconds).buildAsyncTimeout(): _ =>
          for
            visible <- api.fetchVisibleTournaments
            scheduled <- repo.allScheduledDedup
          yield (visible, scheduled)

  object homepage:

    def get(teamIds: List[TeamId]): Fu[List[Tournament]] = for
      base <- sameForEveryone.get(())
      teamTours <- visibleForTeams(teamIds, Minutes(3 * 60), "homepage", Max(3))
    yield teamTours ::: base

    private val sameForEveryone: AsyncLoadingCache[Unit, List[Tournament]] =
      cacheApi.unit[List[Tournament]]("tournamentFeaturing.homepage.sameForEveryone"):
        _.refreshAfterWrite(2.seconds).buildAsyncTimeout(): _ =>
          for
            started <- repo.scheduledNotHourlyStillWorthEntering
            created <- repo.scheduledNotHourlyCreated(Minutes(crud.CrudForm.maxHomepageHours * 60))
          yield (started ::: created)
            .sortBy(_.startsAt.toSeconds)
            .foldLeft(List.empty[Tournament]): (acc, tour) =>
              if !tour.homepageSince.exists(_.isBefore(nowInstant)) then acc
              else if acc.exists(_.similarSchedule(tour)) then acc
              else tour :: acc
            .reverse

  private def visibleForTeams(
      teamIds: List[TeamId],
      ahead: Minutes,
      page: "index" | "homepage",
      max: Max
  ): Fu[List[Tournament]] =
    teamIds.nonEmpty.so:
      repo
        .visibleForTeams(teamIds, ahead, max)
        .monSuccess(lila.mon.tournament.featuring.forTeams(page))
