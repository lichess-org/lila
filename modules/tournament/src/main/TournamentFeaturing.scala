package lila
package tournament

final class TournamentFeaturing(
    api: TournamentApi,
    cached: TournamentCache,
    repo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  object tourIndex:
    def get(forTeamIds: List[TeamId]): Fu[(List[Tournament], VisibleTournaments)] = for
      (visible, scheduled) <- sameForEveryone get ()
      teamVisible          <- repo.visibleForTeams(forTeamIds, 5 * 60)
      forMe = visible add teamVisible
    yield (scheduled, forMe)

    private val sameForEveryone = cacheApi.unit[(VisibleTournaments, List[Tournament])]:
      _.refreshAfterWrite(3.seconds)
        .buildAsyncFuture: _ =>
          for
            visible   <- api.fetchVisibleTournaments
            scheduled <- repo.allScheduledDedup
          yield (visible, scheduled)

  object homepage:
    def get: Fu[List[Tournament]] = cache get ()
    private val cache = cacheApi.unit[List[Tournament]]:
      _.refreshAfterWrite(2 seconds)
        .buildAsyncFuture: _ =>
          repo.scheduledStillWorthEntering zip repo.scheduledCreated(
            crud.CrudForm.maxHomepageHours * 60
          ) map { (started, created) =>
            (started ::: created)
              .sortBy(_.startsAt.toSeconds)
              .foldLeft(List.empty[Tournament]): (acc, tour) =>
                if !canShowOnHomepage(tour) then acc
                else if acc.exists(_ similarTo tour) then acc
                else tour :: acc
              .reverse
          }

    private def canShowOnHomepage(tour: Tournament): Boolean =
      tour.schedule.exists: schedule =>
        tour.startsAt isBefore nowInstant.plusMinutes:
          import Schedule.Freq.*
          val base = schedule.freq match
            case Unique => tour.spotlight.flatMap(_.homepageHours).fold(24 * 60)((_: Int) * 60)
            case Unique | Yearly | Marathon => 24 * 60
            case Monthly | Shield           => 6 * 60
            case Weekly | Weekend           => 3 * 45
            case Daily                      => 1 * 30
            case _                          => 20
          if tour.variant.exotic && schedule.freq != Unique then base / 3 else base
