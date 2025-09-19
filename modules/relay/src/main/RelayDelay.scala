package lila.relay

import chess.format.pgn.PgnStr
import io.mola.galimatias.URL
import scalalib.model.Seconds

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.MultiPgn
import lila.relay.RelayRound.Sync.Upstream

final private class RelayDelay(colls: RelayColls, cacheApi: CacheApi)(using Executor):

  import RelayDelay.*

  def urlSource(url: URL, round: RelayRound, doFetch: URL => Fu[RelayGames]): Fu[RelayGames] =
    fromSource(UrlSource(url), round, () => doFetch(url))

  def internalSource(round: RelayRound, doFetch: => Fu[RelayGames]): Fu[RelayGames] =
    fromSource(InternalSource(round.id), round, () => doFetch)

  def delayedUntil(round: RelayRound): Fu[Option[Instant]] =
    (round.sync.ongoing && !round.hasStarted)
      .so(round.sync.delay)
      .so: delay =>
        round.sync.upstream
          .so:
            case Upstream.Ids(_) => InternalSource(round.id).some
            case Upstream.Users(_) => InternalSource(round.id).some
            case Upstream.Url(url) => UrlSource(url).some
            // only looks at the first URL. Probably won't always work.
            case Upstream.Urls(urls) => urls.headOption.map(UrlSource(_))
          .so: source =>
            store.nextDate.get(source.cacheKey)
          .map:
            _.map(_.plusSeconds(delay.value)).orElse:
              // no moves ever seen from the source yet?
              // we'll have to wait at least `delay` from now, then
              round.startsAtTime.map:
                _.atLeast(nowInstant.plusSeconds(delay.value))

  private def fromSource(
      source: RelaySource,
      round: RelayRound,
      doFetch: () => Fu[RelayGames]
  ): Fu[RelayGames] = for
    latest <- source match
      case UrlSource(url) => dedupCache(url, round, doFetch)
      case _ => doFetch()
    delayed <- round.sync.delayMinusLag match
      case None => fuccess(latest)
      case Some(delay) =>
        store.putIfNew(source.cacheKey, latest)
        store.get(source.cacheKey, delay).map(_ | latest.map(_.resetToSetup))
  yield delayed

  // makes sure that an upstream used by several broadcasts
  // is only pulled from as many times as necessary, and not more.
  private object dedupCache:

    private val cache = CacheApi.scaffeineNoScheduler
      .initialCapacity(8)
      .maximumSize(256)
      .build[URL, GamesSeenBy]()
      .underlying

    def apply(url: URL, round: RelayRound, doFetch: () => Fu[RelayGames]) =
      cache.asMap
        .compute(
          url,
          (_, v) =>
            Option(v) match
              case Some(GamesSeenBy(games, seenBy)) if !seenBy(round.id) =>
                lila.mon.relay.dedup.increment()
                logger.debug(s"Relay dedup cache hit ${round.id} ${round.name} $url")
                GamesSeenBy(games, seenBy + round.id)
              case _ =>
                GamesSeenBy(doFetch(), Set(round.id))
        )
        .games

  private object store:

    private def idOf(key: CacheKey, at: Instant) = s"$key ${at.toSeconds}"
    private val longPast = java.time.Instant.ofEpochMilli(0)

    def putIfNew(key: CacheKey, games: RelayGames): Funit =
      val newPgn = RelayGame.iso.from(games).toPgnStr
      getLatestPgn(key).flatMap:
        case Some(latestPgn) if latestPgn == newPgn => funit
        case _ =>
          val now = nowInstant
          val doc = $doc("_id" -> idOf(key, now), "at" -> now, "pgn" -> newPgn)
          colls.delay:
            _.insert.one(doc).void

    def get(key: CacheKey, delay: Seconds): Fu[Option[RelayGames]] =
      getPgn(key, delay).map2: pgn =>
        RelayGame.iso.to(MultiPgn.split(pgn, Max(999)))

    private def getLatestPgn(key: CacheKey): Fu[Option[PgnStr]] =
      getPgn(key, Seconds(0))

    private def getPgn(key: CacheKey, delay: Seconds): Fu[Option[PgnStr]] =
      colls.delay:
        _.find(
          $doc("_id".$gt(idOf(key, longPast)).$lte(idOf(key, nowInstant.minusSeconds(delay.value)))),
          $doc("pgn" -> true).some
        ).sort($sort.desc("_id"))
          .one[Bdoc]
          .map:
            _.flatMap(_.getAsOpt[PgnStr]("pgn"))

    val nextDate = cacheApi[CacheKey, Option[Instant]](64, "relay.delayedUntil"):
      _.expireAfterWrite(1.second).buildAsyncFuture: key =>
        colls.delay:
          _.primitiveOne[Instant](
            $doc(
              "_id".$gt(idOf(key, longPast)),
              "pgn".$regex("\n1\\. ")
            ),
            $sort.asc("_id"),
            "at"
          )

private object RelayDelay:

  val maxSeconds = Seconds(60 * 60)

  private case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[RelayRoundId])

  opaque type CacheKey = String

  private trait RelaySource:
    def cacheKey: CacheKey

  private case class UrlSource(url: URL) extends RelaySource:
    def cacheKey = url.toString

  private case class InternalSource(roundId: RelayRoundId) extends RelaySource:
    def cacheKey = roundId.value
