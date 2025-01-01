package lila.security

import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.net.Domain
import lila.db.dsl.*

/* An expensive API detecting disposable email.
 * Only hit after trying everything else (DnsApi)
 * and save the result for a long time.
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
          "_id".$regex(s"^$prefix:"),
          "v" -> false
        ),
        _.sec
      )
      .map: ids =>
        val dropSize = prefix.length + 1
        ids.map(_.drop(dropSize))

  private val prefix = "security:check_mail"

  private val cache = mongoCache[Domain.Lower, Boolean](512, prefix, 30.days, _.toString): loader =>
    _.maximumSize(512).buildAsyncFuture(loader(fetch))

  export cache.invalidate

  private def fetch(domain: Domain.Lower): Fu[Boolean] =
    List(fetchFree(domain), fetchPaid(domain))
      .map(_.logFailure(logger).recover(_ => true)) // fetch fail = domain ok
      .parallel
      .map(_.forall(identity)) // ok if both say the domain is ok

  object fetchFree:
    private var rateLimitedUntil = java.time.Instant.EPOCH
    def apply(domain: Domain.Lower): Fu[Boolean] =
      if rateLimitedUntil.isAfterNow then fuccess(true)
      else
        val url = s"https://api.mailcheck.ai/domain/$domain"
        ws.url(url)
          .get()
          .withTimeout(8.seconds, "mailcheck.fetch")
          .map: res =>
            if res.status == 429
            then
              logger.info(s"Mailcheck rate limited $url")
              rateLimitedUntil = nowInstant.plusMinutes(5)
              true
            else
              (for
                js <- res.body[JsValue].asOpt[JsObject]
                if res.status == 200
                disposable <- js.boolean("disposable")
              yield
                val ok = !disposable
                logger.info:
                  s"Mailcheck $domain = $ok {disposable:$disposable}"
                ok
              ).getOrElse:
                throw lila.core.lilaism.LilaException(s"$url ${res.status} ${res.body[String].take(200)}")
          .monTry(res => _.security.mailcheckApi.fetch(res.isSuccess, res.getOrElse(true)))

  private def fetchPaid(domain: Domain.Lower): Fu[Boolean] =
    val url = s"https://verifymail.io/api/$domain"
    ws.url(url)
      .withQueryStringParameters("key" -> config.key.value)
      .get()
      .withTimeout(8.seconds, "VerifyMail.fetch")
      .map: res =>
        (for
          js <- res.body[JsValue].asOpt[JsObject]
          if res.status == 200
          block <- js.boolean("block")
          disposable = ~js.boolean("disposable")
          privacy    = ~js.boolean("privacy")
        yield
          val ok = !disposable
          logger.info:
            s"VerifyMail $domain = $ok {block:$block,disposable:$disposable,privacy:$privacy}"
          ok
        ).getOrElse:
          throw lila.core.lilaism.LilaException(s"$url ${res.status} ${res.body[String].take(200)}")
      .monTry(res => _.security.verifyMailApi.fetch(res.isSuccess, res.getOrElse(true)))
