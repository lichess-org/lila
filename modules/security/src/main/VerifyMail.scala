package lila.security

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyReadables.*

import lila.common.Domain
import lila.db.dsl.*

/* An expensive API detecting disposable email.
 * Only hit after trying everything else (DnsApi)
 * and save the result forever.
 * https://verifymail.io/api-documentation
 */
final private class VerifyMail(
    ws: StandaloneWSClient,
    config: SecurityConfig.VerifyMail,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor, Scheduler):

  def apply(domain: Domain.Lower): Fu[Boolean] =
    if config.key.value.isEmpty then fuccess(true)
    else
      cache
        .get(domain)
        .withTimeoutDefault(2.seconds, true)
        .recover { case e: Exception =>
          logger.warn(s"VerifyMail $domain ${e.getMessage}", e)
          true
        }

  // expensive
  private[security] def fetchAllBlocked: Fu[List[String]] =
    cache.coll
      .distinctEasy[String, List](
        "_id",
        $doc(
          "_id" $regex s"^$prefix:",
          "v" -> false
        ),
        _.sec
      )
      .map: ids =>
        val dropSize = prefix.length + 1
        ids.map(_ drop dropSize)

  private val prefix = "security:check_mail"

  private val cache = mongoCache[Domain.Lower, Boolean](512, prefix, 100 days, _.toString): loader =>
    _.maximumSize(512).buildAsyncFuture(loader(fetch))

  export cache.invalidate

  private def fetch(domain: Domain.Lower): Fu[Boolean] =
    val url = s"https://verifymail.io/api/$domain"
    ws.url(url)
      .withQueryStringParameters("key" -> config.key.value)
      .get()
      .withTimeout(8.seconds, "VerifyMail.fetch")
      .map: res =>
        if res.status == 200 then
          def readBool(key: String) = ~(res.body[JsValue] \ key).asOpt[Boolean]
          val block                 = readBool("block")
          val disposable            = readBool("disposable")
          val privacy               = readBool("privacy")
          val ok                    = !block && !disposable
          logger.info:
            s"VerifyMail $domain = $ok {block:$block,disposable:$disposable,privacy:$privacy}"
          ok
        else throw lila.base.LilaException(s"$url ${res.status} ${res.body[String] take 200}")
      .monTry(res => _.security.verifyMailApi.fetch(res.isSuccess, res.getOrElse(true)))
