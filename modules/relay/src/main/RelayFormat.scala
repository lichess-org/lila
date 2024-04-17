package lila.relay

import chess.format.pgn.PgnStr
import com.softwaremill.tagging.*
import io.mola.galimatias.URL
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.{
  DefaultWSProxyServer,
  StandaloneWSClient,
  StandaloneWSRequest,
  StandaloneWSResponse
}

import scala.util.matching.Regex

import lila.core.lilaism.{ LilaInvalid, LilaException }
import lila.core.config.{ Credentials, HostPort }
import lila.memo.CacheApi.*
import lila.memo.{ CacheApi, SettingStore }
import lila.study.MultiPgn

final private class RelayFormatApi(
    ws: StandaloneWSClient,
    cacheApi: CacheApi,
    proxyCredentials: SettingStore[Option[Credentials]] @@ ProxyCredentials,
    proxyHostPort: SettingStore[Option[HostPort]] @@ ProxyHostPort,
    proxyDomainRegex: SettingStore[Regex] @@ ProxyDomainRegex
)(using Executor):

  import RelayFormat.*
  import RelayRound.Sync.UpstreamUrl

  private val cache = cacheApi[(UpstreamUrl.WithRound, CanProxy), RelayFormat](64, "relay.format"):
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture: (url, proxy) =>
        guessFormat(url)(using proxy)

  def get(upstream: UpstreamUrl.WithRound)(using proxy: CanProxy): Fu[RelayFormat] =
    cache.get(upstream -> proxy)

  def refresh(upstream: UpstreamUrl.WithRound): Unit =
    CanProxy
      .from(List(false, true))
      .foreach: proxy =>
        cache.invalidate(upstream -> proxy)

  private def guessFormat(upstream: UpstreamUrl.WithRound)(using CanProxy): Fu[RelayFormat] = {

    val originalUrl = URL.parse(upstream.url)

    // http://view.livechesscloud.com/ed5fb586-f549-4029-a470-d590f8e30c76
    def guessLcc(url: URL)(using CanProxy): Fu[Option[RelayFormat]] =
      url.toString match
        case UpstreamUrl.LccRegex(id) =>
          guessManyFiles:
            URL.parse:
              s"http://1.pool.livechesscloud.com/get/$id/round-${upstream.round | 1}/index.json"
        case _ => fuccess(none)

    def guessSingleFile(url: URL)(using CanProxy): Fu[Option[RelayFormat]] =
      List(
        url.some,
        (!url.pathSegments.contains(mostCommonSingleFileName)).option(addPart(url, mostCommonSingleFileName))
      ).flatten.distinct
        .findM(looksLikePgn)
        .dmap2: (u: URL) =>
          SingleFile(pgnDoc(u))

    def guessManyFiles(url: URL)(using CanProxy): Fu[Option[RelayFormat]] =
      (List(url) ::: mostCommonIndexNames
        .filterNot(url.pathSegments.contains)
        .map(addPart(url, _)))
        .findM(looksLikeJson)
        .flatMapz: index =>
          val jsonUrl = (n: Int) => jsonDoc(replaceLastPart(index, s"game-$n.json"))
          val pgnUrl  = (n: Int) => pgnDoc(replaceLastPart(index, s"game-$n.pgn"))
          looksLikeJson(jsonUrl(1).url)
            .recover:
              case NotFound(_) => false
            .map(_.option(jsonUrl))
            .orElse:
              looksLikePgn(pgnUrl(1).url)
                .recover:
                  case NotFound(_) => false
                .map(_.option(pgnUrl))
            .dmap2:
              ManyFiles(index, _)
            .dmap(_.orElse(ManyFilesLater(index).some))

    guessLcc(originalUrl)
      .orElse(guessSingleFile(originalUrl))
      .orElse(guessManyFiles(originalUrl))
      .orFailWith(LilaInvalid(s"No games found at $originalUrl"))
  }.addEffect { format =>
    logger.info(s"guessed format of $upstream: $format")
  }

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
        else if res.status == 404 then fufail(NotFound(url.toString))
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
      .so: pgn =>
        val data = lila.core.game.ImportData(pgn, none)
        lila.game.importer.parseImport(data, none).isRight

  private def looksLikePgn(url: URL)(using CanProxy): Fu[Boolean] = httpGet(url).map(looksLikePgn)

  private def looksLikeJson(body: String): Boolean =
    try Json.parse(body) != JsNull
    catch case _: Exception => false
  private def looksLikeJson(url: URL)(using CanProxy): Fu[Boolean] = httpGet(url).map(looksLikeJson)

sealed private trait RelayFormat

private object RelayFormat:

  opaque type CanProxy = Boolean
  object CanProxy extends YesNo[CanProxy]

  enum DocFormat:
    case Json, Pgn

  case class RemoteDoc(url: URL, format: DocFormat)

  def jsonDoc(url: URL) = RemoteDoc(url, DocFormat.Json)
  def pgnDoc(url: URL)  = RemoteDoc(url, DocFormat.Pgn)

  case class SingleFile(doc: RemoteDoc) extends RelayFormat

  type GameNumberToDoc = Int => RemoteDoc

  case class ManyFiles(jsonIndex: URL, game: GameNumberToDoc) extends RelayFormat:
    override def toString = s"Manyfiles($jsonIndex, ${game(0)})"

  // there will be game files with names like "game-1.json" or "game-1.pgn"
  // but not at the moment. The index is still useful.
  case class ManyFilesLater(jsonIndex: URL) extends RelayFormat:
    override def toString = s"ManyfilesLater($jsonIndex)"

  def addPart(url: URL, part: String)             = url.withPath(s"${url.path}/$part")
  def replaceLastPart(url: URL, withPart: String) = url.withPath(s"${url.path}/../$withPart")

  val mostCommonSingleFileName = "games.pgn"
  val mostCommonIndexNames     = List("round.json", "index.json")

  case class NotFound(message: String) extends LilaException
