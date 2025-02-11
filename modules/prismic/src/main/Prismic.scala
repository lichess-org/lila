package lila.prismic

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.json.JsValue
import play.api.libs.ws._

import com.github.blemale.scaffeine.AsyncCache
import com.github.blemale.scaffeine.AsyncLoadingCache

final class Prismic(
    config: PrismicConfig,
    ws: WSClient,
)(implicit ec: ExecutionContext) {

  def get: Future[Api] = apiCache get config.apiUrl

  private[prismic] val apiCache: AsyncLoadingCache[String, Api] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterWrite(30 seconds)
      .buildAsyncFuture[String, Api](fetch)

  private[prismic] val formCache: AsyncCache[String, Response] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterWrite(1 hour)
      .buildAsync[String, Response]()

  private def fetch(endpoint: String): Future[Api] =
    ws.url(endpoint)
      .withHttpHeaders("Accept" -> "application/json")
      .get()
      .map { resp =>
        resp.status match {
          case 200 => resp.body[JsValue]
          case 401 =>
            throw UnexpectedError(
              "Authorization error, but not URL was provided",
            )
          case err =>
            throw UnexpectedError(
              s"Got an HTTP error $err (${resp.statusText})",
            )
        }
      }
      .map { json =>
        new Api(
          ApiData.reader
            .reads(json)
            .getOrElse(sys.error(s"Error while parsing API document: $json")),
          formCache,
        )
      }
}
