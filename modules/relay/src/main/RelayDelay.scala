package lila.relay

import lila.relay.RelayRound.Sync.UpstreamUrl
import lila.memo.CacheApi
import lila.common.Seconds

final private class RelayDelay:

  import RelayDelay.*

  // The goal of this is to make sure that an upstream used by several broadcast
  // is only pulled from as many times as necessary, and not more.
  private val dedupCache: Cache[UpstreamUrl, GamesSeenBy] = CacheApi.scaffeineNoScheduler
    .initialCapacity(4)
    .maximumSize(32)
    .build[UpstreamUrl, GamesSeenBy]()
    .underlying

  private object store:
    private val cache = CacheApi.scaffeine
      .expireAfterWrite((maxSeconds.value + 60).seconds)
      .build[UpstreamUrl, RelayGames]()

    def apply(url: UpstreamUrl, games: Fu[RelayGames]): Fu[RelayGames] =
      games.dmap { g =>
        cache.put(url, g)
        g
      }

    def get(url: UpstreamUrl): Fu[Option[RelayGames]] = fuccess(cache.getIfPresent(url))

  def apply(
      url: UpstreamUrl,
      rt: RelayRound.WithTour,
      doFetchUrl: (UpstreamUrl, Int) => Fu[RelayGames]
  ): Fu[RelayGames] =
    val latest = dedupCache.asMap
      .compute(
        url,
        (_, v) =>
          Option(v) match
            case Some(GamesSeenBy(games, seenBy)) if !seenBy(rt.round.id) =>
              GamesSeenBy(games, seenBy + rt.round.id)
            case _ =>
              GamesSeenBy(doFetchUrl(url, RelayFetch.maxChapters(rt.tour)), Set(rt.round.id))
      )
      .games
    latest

private object RelayDelay:
  val maxSeconds = Seconds(1800)

  private case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[RelayRoundId])
