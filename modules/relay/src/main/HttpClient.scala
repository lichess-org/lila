package lila.relay

import java.nio.charset.{ Charset, StandardCharsets }
import io.mola.galimatias.URL
import play.api.libs.ws.*
import play.shaded.ahc.org.asynchttpclient.util.HttpUtils.extractContentTypeCharsetAttribute

import lila.core.lilaism.LilaException

/* Extra generic features for play WS client,
 * without any knowledge of broadcast specifics.
 * This could be moved for reuse later on.
 * - Proxies
 * - Etag cache
 */
private final class HttpClient(
    ws: StandaloneWSClient,
    cacheApi: lila.memo.CacheApi,
    proxySelector: ProxySelector
)(using Executor):

  import HttpClient.*

  val etagCache = cacheApi.notLoadingSync[URL, (Body, Etag)](256, "relay.fetch.etagCache"):
    _.expireAfterWrite(10.minutes).build()

  def get(url: URL)(using CanProxy): Fu[Body] =
    etagCache
      .getIfPresent(url)
      .match
        case None =>
          fetchBodyAndEtag(url, none)
        case Some(prevBody, prevEtag) =>
          fetchBodyAndEtag(url, prevEtag.some).map: (newBody, newEtag) =>
            val body = if newBody.isEmpty && newEtag.has(prevEtag) then prevBody.some else newBody
            (body, newEtag)
      .map: (body, etag) =>
        (body, etag).mapN((b, e) => etagCache.put(url, b -> e))
        ~body

  private def fetchBodyAndEtag(url: URL, etag: Option[Etag])(using
      CanProxy
  ): Fu[(Option[Body], Option[Etag])] =
    val req = etag.foldLeft(toRequest(url))((req, etag) => req.addHttpHeaders("If-None-Match" -> etag))
    fetchResponse(req)
      .map: res =>
        val newEtag = extractEtagValue(res)
        if res.status == 304
        then none                         -> newEtag.orElse(etag)
        else decodeResponseBody(res).some -> newEtag
      .recoverWith:
        case Status(400, _) if etag.isDefined =>
          val prevEtag = etag.get // terrible, I wish it could be extracted from the match above
          fetchBodyAndEtag(url, none)
            .addEffects: res =>
              val success = if res.isSuccess then "succeeded" else "failed"
              logger.info(s"Retrying $url without etag $prevEtag -> $success")
            .map: (body, etag) =>
              (body, etag.filter(_ != prevEtag))

  // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag#w
  private def extractEtagValue(res: StandaloneWSResponse): Option[Etag] =
    res
      .header("Etag")
      .map: etag =>
        if etag.startsWith("W/\"") then etag.drop(3).dropRight(1) else etag

  private def fetchResponse(req: StandaloneWSRequest): Fu[StandaloneWSResponse] =
    Future
      .fromTry(lila.common.url.parse(req.url))
      .flatMap: url =>
        req
          .get()
          .monValue: res =>
            _.relay.httpGet(
              res.status,
              url.host.toString,
              etag = monitorEtagHit(req, res),
              req.proxyServer.map(_.host)
            )
          .flatMap: res =>
            if res.status == 200 || res.status == 304 then fuccess(res)
            else fufail(Status(res.status, url))

  private def decodeResponseBody(res: StandaloneWSResponse): Body =
    val charset = Option(extractContentTypeCharsetAttribute(res.contentType))
      .orElse(res.contentType.startsWith("text/").option(StandardCharsets.ISO_8859_1))
    charset match
      case None        => lila.common.String.charset.guessAndDecode(res.bodyAsBytes)
      case Some(known) => res.bodyAsBytes.decodeString(known)

  private def toRequest(url: URL)(using CanProxy): StandaloneWSRequest =
    val req = ws
      .url(url.toString)
      .withRequestTimeout(5.seconds)
      .withFollowRedirects(false)
    proxySelector(url).foldLeft(req)(_ withProxyServer _)

  private def monitorEtagHit(req: StandaloneWSRequest, res: StandaloneWSResponse): String =
    (req.header("If-None-Match"), res.header("Etag")) match
      case (None, None)                      => "none"  // endpoint doesn't support Etag
      case (None, Some(_))                   => "first" // local cache is cold
      case (Some(_), _) if res.status == 304 => "hit"   // cache hit
      case (Some(_), Some(_))                => "miss"  // new data from the endpoint
      case (Some(_), None) => "fail" // we sent an etag but the endpoint doesn't support it?

private object HttpClient:
  type Etag = String
  type Body = String
  case class Status(code: Int, url: URL) extends LilaException:
    override val message = s"$code: $url"
