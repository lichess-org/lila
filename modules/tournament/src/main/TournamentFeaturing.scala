package lila
package tournament

final class TournamentFeaturing(
    api: TournamentApi,
    cached: TournamentCache,
    repo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  object tourIndex:
    def get: Fu[(VisibleTournaments, List[Tournament])] = cache get ()
    private val cache = cacheApi.unit[(VisibleTournaments, List[Tournament])]:
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
        .buildAsyncFuture(_ => repo.onHomepage)
