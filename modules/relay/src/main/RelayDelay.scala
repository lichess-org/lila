package lila.relay

import chess.format.pgn.PgnStr
import io.mola.galimatias.URL
import scalalib.model.Seconds

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.MultiPgn

final private class RelayDelay(colls: RelayColls)(using Executor):

  import RelayDelay.*

  def apply(
      url: URL,
      round: RelayRound,
      doFetchUrl: URL => Fu[RelayGames]
  ): Fu[RelayGames] =
    dedupCache(url, round, () => doFetchUrl(url))
      .flatMap: latest =>
        round.sync.delay match
          case Some(delay) if delay > 0 => store.get(url, delay).map(_ | latest.map(_.resetToSetup))
          case _                        => fuccess(latest)

  // makes sure that an upstream used by several broadcasts
  // is only pulled from as many times as necessary, and not more.
  private object dedupCache:

    private val cache = CacheApi.scaffeineNoScheduler
      .initialCapacity(8)
      .maximumSize(128)
      .build[String, GamesSeenBy]()
      .underlying

    def apply(url: URL, round: RelayRound, doFetch: () => Fu[RelayGames]) =
      cache.asMap
        .compute(
          url.toString,
          (_, v) =>
            Option(v) match
              case Some(GamesSeenBy(games, seenBy)) if !seenBy(round.id) =>
                lila.mon.relay.dedup.increment()
                GamesSeenBy(games, seenBy + round.id)
              case _ =>
                val futureGames = doFetch().addEffect: games =>
                  if round.sync.hasDelay then store.putIfNew(url, games)
                GamesSeenBy(futureGames, Set(round.id))
        )
        .games

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
          $doc(
            "_id".$gt(idOf(url, longPast)).$lte(idOf(url, nowInstant.minusSeconds(delay.value)))
          ),
          $doc("pgn" -> true).some
        ).sort($sort.desc("_id"))
          .one[Bdoc]
          .map:
            _.flatMap(_.getAsOpt[PgnStr]("pgn"))

private object RelayDelay:
  val maxSeconds = Seconds(60 * 60)

  private case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[RelayRoundId])
