package lila
package tournament

import com.github.blemale.scaffeine.AsyncLoadingCache
import lila.memo.CacheApi.buildAsyncTimeout

final class TournamentFeaturing(
    api: TournamentApi,
    repo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  object tourIndex:
    def get(teamIds: List[TeamId]): Fu[(List[Tournament], VisibleTournaments)] = for
      (base, scheduled) <- sameForEveryone.get(())
      teamTours <- visibleForTeams(teamIds, 5 * 60, "index")
      forMe = base.add(teamTours)
    yield (scheduled, forMe)

    private val sameForEveryone = cacheApi.unit[(VisibleTournaments, List[Tournament])]:
      _.refreshAfterWrite(3.seconds).buildAsyncTimeout(): _ =>
        for
          visible <- api.fetchVisibleTournaments
          scheduled <- repo.allScheduledDedup
        yield (visible, scheduled)

  object homepage:

    def get(teamIds: List[TeamId]): Fu[List[Tournament]] = for
      base <- sameForEveryone.get(())
      teamTours <- visibleForTeams(teamIds, 3 * 60, "homepage")
    yield teamTours ::: base

    private val sameForEveryone: AsyncLoadingCache[Unit, List[Tournament]] = cacheApi.unit[List[Tournament]]:
      _.refreshAfterWrite(2.seconds).buildAsyncTimeout(): _ =>
        for
          started <- repo.scheduledStillWorthEntering
          created <- repo.scheduledCreated(crud.CrudForm.maxHomepageHours * 60)
        yield (started ::: created)
          .sortBy(_.startsAt.toSeconds)
          .foldLeft(List.empty[Tournament]): (acc, tour) =>
            if !tour.homepageSince.exists(_.isBefore(nowInstant)) then acc
            else if acc.exists(_.similarSchedule(tour)) then acc
            else tour :: acc
          .reverse

  private def visibleForTeams(
      teamIds: List[TeamId],
      aheadMinutes: Int,
      page: "index" | "homepage"
  ): Fu[List[Tournament]] =
    teamIds.nonEmpty.so:
      repo
        .visibleForTeams(teamIds, aheadMinutes)
        // .map:
        //   _.foldLeft(List.empty[Tournament]): (dedup, tour) =>
        //     if dedup.exists(_ sameNameAndTeam tour) then dedup
        //     else tour :: dedup
        //   .reverse
        .monSuccess(_.tournament.featuring.forTeams(page))
