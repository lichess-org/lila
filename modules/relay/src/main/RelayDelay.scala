package lila.relay

import lila.relay.RelayRound.Sync.UpstreamUrl
import lila.memo.CacheApi
import lila.common.Seconds
import com.github.benmanes.caffeine.cache.Cache
import lila.db.dsl.{ *, given }
import lila.study.MultiPgn
import chess.format.pgn.PgnStr

final private class RelayDelay(colls: RelayColls)(using Executor):

  import RelayDelay.*

  def apply(
      url: UpstreamUrl,
      rt: RelayRound.WithTour,
      doFetchUrl: (UpstreamUrl, Int) => Fu[RelayGames]
  ): Fu[RelayGames] =
    dedupCache.asMap
      .compute(
        url,
        (_, v) =>
          Option(v) match
            case Some(GamesSeenBy(games, seenBy)) if !seenBy(rt.round.id) =>
              GamesSeenBy(games, seenBy + rt.round.id)
            case _ =>
              val futureGames = doFetchUrl(url, RelayFetch.maxChapters(rt.tour)).addEffect:
                store.putIfNew(url, _)
              GamesSeenBy(futureGames, Set(rt.round.id))
      )
      .games
      .flatMap: latest =>
        rt.round.sync.delay match
          case Some(delay) if delay > 0 => store.get(url, delay).map(_ | Vector.empty)
          case _                        => fuccess(latest)

  // The goal of this is to make sure that an upstream used by several broadcast
  // is only pulled from as many times as necessary, and not more.
  private val dedupCache: Cache[UpstreamUrl, GamesSeenBy] = CacheApi.scaffeineNoScheduler
    .initialCapacity(4)
    .maximumSize(32)
    .build[UpstreamUrl, GamesSeenBy]()
    .underlying

  private object store:

    private def idOf(upstream: UpstreamUrl, at: Instant) = s"${upstream.url} ${at.toSeconds}"
    private val longPast                                 = java.time.Instant.ofEpochMilli(0)

    def putIfNew(upstream: UpstreamUrl, games: RelayGames): Funit =
      val newPgn = RelayGame.iso.from(games).toPgnStr
      getLatestPgn(upstream).flatMap:
        case Some(latestPgn) if latestPgn == newPgn => funit
        case _ =>
          val doc = $doc("_id" -> idOf(upstream, nowInstant), "at" -> nowInstant, "pgn" -> newPgn)
          colls.delay:
            _.insert.one(doc).void

    def get(upstream: UpstreamUrl, delay: Seconds): Fu[Option[RelayGames]] =
      getPgn(upstream, delay).map2: pgn =>
        RelayGame.iso.to(MultiPgn.split(pgn, 999))

    private def getLatestPgn(upstream: UpstreamUrl): Fu[Option[PgnStr]] = getPgn(upstream, Seconds(0))

    private def getPgn(upstream: UpstreamUrl, delay: Seconds): Fu[Option[PgnStr]] =
      colls.delay:
        _.find(
          $doc("_id" $gt idOf(upstream, longPast) $lte idOf(upstream, nowInstant.minusSeconds(delay.value))),
          $doc("pgn" -> true).some
        ).sort($sort desc "_id")
          .one[Bdoc]
          .map:
            _.flatMap(_.getAsOpt[PgnStr]("pgn"))

private object RelayDelay:
  val maxSeconds = Seconds(1800)

  private case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[RelayRoundId])
