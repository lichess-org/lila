package lila.relay

import chess.format.pgn.PgnStr
import io.mola.galimatias.URL
import scalalib.model.Seconds

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.MultiPgn

private sealed abstract class RelaySource[Source, CacheKey](val source: Source, val cacheKey: CacheKey)
private final class UrlSource(url: URL) extends RelaySource(url, url)
private final class GamesSource(games: List[GameId], roundId: RelayRoundId)
    extends RelaySource(games, roundId)

final private class RelayDelay(colls: RelayColls)(using Executor):

  import RelayDelay.*

  def fromSource[Source, CacheKey](
      source: RelaySource[Source, CacheKey],
      round: RelayRound,
      doFetch: Source => Fu[RelayGames]
  ): Fu[RelayGames] =
    dupCache(source, round, () => doFetch(source.source))
      .flatMap: latest =>
        round.sync.delayMinusLag match
          case Some(delay) if delay > 0 => store.get(source, delay).map(_ | latest.map(_.resetToSetup))
          case _                        => fuccess(latest)

  def fromGames(round: RelayRound, doFetchGames: => Fu[RelayGames]): Fu[RelayGames] =
    doFetchGames

  // makes sure that an upstream used by several broadcasts
  // is only pulled from as many times as necessary, and not more.
  private object dedupCache:

    private val cache = CacheApi.scaffeineNoScheduler
      .initialCapacity(8)
      .maximumSize(256)
      .build[RelaySource, GamesSeenBy]()
      .underlying

    def apply(source: RelaySource, round: RelayRound, doFetch: () => Fu[RelayGames]) =
      cache.asMap
        .compute(
          source,
          (_, v) =>
            Option(v) match
              case Some(GamesSeenBy(games, seenBy)) if !seenBy(round.id) =>
                lila.mon.relay.dedup.increment()
                logger.debug(s"Relay dedup cache hit ${round.id} ${round.name} $source")
                GamesSeenBy(games, seenBy + round.id)
              case _ =>
                GamesSeenBy(doFetch(), Set(round.id))
        )
        .games
        .addEffect: games =>
          if round.sync.delayMinusLag.isDefined then store.putIfNew(source, games)

  private object store:

    private def idOf(url: URL, at: Instant) = s"$url ${at.toSeconds}"
    private val longPast                    = java.time.Instant.ofEpochMilli(0)

    def putIfNew(url: URL, games: RelayGames): Funit =
      val newPgn = RelayGame.iso.from(games).toPgnStr
      getLatestPgn(url).flatMap:
        case Some(latestPgn) if latestPgn == newPgn => funit
        case _ =>
          val now = nowInstant
          val doc = $doc("_id" -> idOf(url, now), "at" -> now, "pgn" -> newPgn)
          colls.delay:
            _.insert.one(doc).void

    def get(url: URL, delay: Seconds): Fu[Option[RelayGames]] =
      getPgn(url, delay).map2: pgn =>
        RelayGame.iso.to(MultiPgn.split(pgn, Max(999)))

    private def getLatestPgn(url: URL): Fu[Option[PgnStr]] =
      getPgn(url, Seconds(0))

    private def getPgn(url: URL, delay: Seconds): Fu[Option[PgnStr]] =
      colls.delay:
        _.find(
          $doc("_id".$gt(idOf(url, longPast)).$lte(idOf(url, nowInstant.minusSeconds(delay.value)))),
          $doc("pgn" -> true).some
        ).sort($sort.desc("_id"))
          .one[Bdoc]
          .map:
            _.flatMap(_.getAsOpt[PgnStr]("pgn"))

private object RelayDelay:
  val maxSeconds = Seconds(60 * 60)

  private case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[RelayRoundId])
