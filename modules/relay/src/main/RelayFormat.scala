package lila.relay

import chess.format.pgn.PgnStr
import com.softwaremill.tagging.*
import io.mola.galimatias.URL
import play.api.libs.json.*
import play.api.libs.ws.{
  DefaultWSProxyServer,
  StandaloneWSClient,
  StandaloneWSRequest,
  StandaloneWSResponse
}

import scala.util.matching.Regex

import lila.core.config.{ Credentials, HostPort }
import lila.core.lilaism.{ LilaException, LilaInvalid }
import lila.memo.CacheApi.*
import lila.memo.{ CacheApi, SettingStore }
import lila.study.MultiPgn

final private class RelayFormatApi(
    roundRepo: RelayRoundRepo,
    ws: StandaloneWSClient,
    cacheApi: CacheApi,
    proxyCredentials: SettingStore[Option[Credentials]] @@ ProxyCredentials,
    proxyHostPort: SettingStore[Option[HostPort]] @@ ProxyHostPort,
    proxyDomainRegex: SettingStore[Regex] @@ ProxyDomainRegex
)(using Executor):

  import RelayFormat.*

  private val cache = cacheApi[(URL, CanProxy), RelayFormat](64, "relay.format"):
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture: (url, proxy) =>
        guessFormat(url)(using proxy)

  def get(url: URL)(using proxy: CanProxy): Fu[RelayFormat] =
    cache.get(url -> proxy)

  def refresh(url: URL): Unit =
    CanProxy
      .from(List(false, true))
      .foreach: proxy =>
        cache.invalidate(url -> proxy)

  private def guessFormat(url: URL)(using CanProxy): Fu[RelayFormat] =
    RelayRound.Sync.Upstream
      .Url(url)
      .lcc
      .match
        case Some(lcc) =>
          looksLikeJson(lcc.indexUrl).flatMapz:
            looksLikeJson(lcc.gameUrl(1))
              .recoverDefault(false)(_ => ())
              .map:
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

  private[relay] def httpGet(url: URL)(using CanProxy): Fu[String] =
    httpGetResponse(url).map(_.body)

  private[relay] def httpGetAndGuessCharset(url: URL)(using CanProxy): Fu[String] =
    httpGetResponse(url).map: res =>
      responseHeaderCharset(res) match
        case None        => lila.common.String.charset.guessAndDecode(res.bodyAsBytes)
        case Some(known) => res.bodyAsBytes.decodeString(known)

  private def responseHeaderCharset(res: StandaloneWSResponse): Option[java.nio.charset.Charset] =
    import play.shaded.ahc.org.asynchttpclient.util.HttpUtils
    Option(HttpUtils.extractContentTypeCharsetAttribute(res.contentType)).orElse:
      res.contentType.startsWith("text/").option(java.nio.charset.StandardCharsets.ISO_8859_1)

  private def httpGetResponse(url: URL)(using CanProxy): Future[StandaloneWSResponse] =
    val (req, proxy) = addProxy(url):
      ws.url(url.toString)
        .withRequestTimeout(5.seconds)
        .withFollowRedirects(false)
    req
      .get()
      .flatMap: res =>
        if res.status == 200 then fuccess(res)
        else if res.status == 404 then fufail(NotFound(url))
        else fufail(s"[${res.status}] $url")
      .monSuccess(_.relay.httpGet(url.host.toString, proxy))

  private def addProxy(url: URL)(ws: StandaloneWSRequest)(using
      allowed: CanProxy
  ): (StandaloneWSRequest, Option[String]) =
    def server = for
      hostPort <- proxyHostPort.get()
      if allowed.yes
      proxyRegex = proxyDomainRegex.get()
      if proxyRegex.toString.nonEmpty
      if proxyRegex.unanchored.matches(url.host.toString)
      creds = proxyCredentials.get()
    yield DefaultWSProxyServer(
      host = hostPort.host,
      port = hostPort.port,
      principal = creds.map(_.user),
      password = creds.map(_.password.value)
    )
    server.foldLeft(ws)(_ withProxyServer _) -> server.map(_.host)

  private def looksLikePgn(body: String)(using CanProxy): Boolean =
    MultiPgn
      .split(PgnStr(body), Max(1))
      .value
      .headOption
      .so(lila.game.importer.parseImport(_, none).isRight)

  private def looksLikePgn(url: URL)(using CanProxy): Fu[Boolean] = httpGet(url).map(looksLikePgn)

  private def looksLikeJson(body: String): Boolean =
    try Json.parse(body) != JsNull
    catch case _: Exception => false
  private def looksLikeJson(url: URL)(using CanProxy): Fu[Boolean] = httpGet(url).map(looksLikeJson)

private enum RelayFormat:
  case Round(id: RelayRoundId)
  case SingleFile(url: URL)
  case LccWithGames(lcc: RelayRound.Sync.Lcc)
  // there will be game files with names like "game-1.json" or "game-1.pgn"
  // but not at the moment. The index is still useful.
  case LccWithoutGames(lcc: RelayRound.Sync.Lcc)

private object RelayFormat:

  opaque type CanProxy = Boolean
  object CanProxy extends YesNo[CanProxy]

  case class NotFound(url: URL) extends LilaException:
    override val message = s"404: $url"
