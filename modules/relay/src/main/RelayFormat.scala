package lila.relay

import chess.format.pgn.PgnStr
import io.mola.galimatias.URL
import play.api.libs.json.*

import lila.core.lilaism.LilaInvalid
import lila.memo.CacheApi.*

final private class RelayFormatApi(
    roundRepo: RelayRoundRepo,
    cacheApi: lila.memo.CacheApi,
    http: HttpClient
)(using Executor):

  import RelayFormat.*
  import HttpClient.*

  private val cache = cacheApi[(URL, CanProxy), RelayFormat](64, "relay.format"):
    _.expireAfterWrite(5.minutes).buildAsyncFuture: (url, proxy) =>
      guessFormat(url)(using proxy)

  def get(url: URL)(using proxy: CanProxy): Fu[RelayFormat] =
    cache.get(url -> proxy)

  def refresh(url: URL): Unit =
    CanProxy
      .from(List(false, true))
      .foreach: proxy =>
        cache.invalidate(url -> proxy)

  private def guessFormat(url: URL)(using CanProxy): Fu[RelayFormat] =
    import RelayRound.Sync.url.*
    url.lcc
      .match
        case Some(lcc) =>
          looksLikeJson(lcc.indexUrl).flatMapz:
            def gameExists(index: Int) = looksLikeJson(lcc.gameUrl(index)).recoverDefault(false)(_ => ())
            (gameExists(1) >>| gameExists(2)).map:
              if _ then LccWithGames(lcc).some
              else LccWithoutGames(lcc).some
        case None =>
          guessRelayRound(url).orElse:
            looksLikePgn(url).mapz(SingleFile(url).some)
      .orFailWith(LilaInvalid(s"No games found at $url"))
      .addEffect: format =>
        logger.info(s"guessed format of $url: $format")

  private def guessRelayRound(url: URL): Fu[Option[RelayFormat.Round]] =
    RelayRound.Sync.Upstream
      .Url(url)
      .roundId
      .so: id =>
        roundRepo.exists(id).map(_.option(RelayFormat.Round(id)))

  private def looksLikePgn(body: String)(using CanProxy): Boolean =
    lila.study.MultiPgn
      .split(PgnStr(body), Max(1))
      .value
      .headOption
      .so(lila.game.importer.parseImport(_, none).isRight)

  private def looksLikePgn(url: URL)(using CanProxy): Fu[Boolean] = http.get(url).map(looksLikePgn)

  private def looksLikeJson(body: String): Boolean =
    try Json.parse(body) != JsNull
    catch case _: Exception => false
  private def looksLikeJson(url: URL)(using CanProxy): Fu[Boolean] = http.get(url).map(looksLikeJson)

private enum RelayFormat:
  case Round(id: RelayRoundId)
  case SingleFile(url: URL)
  case LccWithGames(lcc: RelayRound.Sync.Lcc)
  // there will be game files with names like "game-1.json" or "game-1.pgn"
  // but not at the moment. The index is still useful.
  case LccWithoutGames(lcc: RelayRound.Sync.Lcc)
