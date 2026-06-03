package lila.security

import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.net.Domain

import lila.mon.extensions.*

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

  export cache.invalidate

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

  // if a positive value is cached, recompute it.
  // email verification services can give false negatives at first
  def refreshIfOk(domain: Domain.Lower): Funit =
    cache
      .dbValue(domain)
      .flatMapz: (v, age) =>
        (v && age.toMinutes > 20).so:
          for
            _ <- cache.invalidate(domain)
            ok <- apply(domain)
          yield logger.info(s"VerifyMail $domain refreshed -> $ok")

  private val cache =
    mongoCache.noHeap[Domain.Lower, Boolean]("security:check_mail", 3.days, _.toString): domain =>
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
          .monTry(res => lila.mon.security.mailcheckApi.fetch(res.isSuccess, res.getOrElse(true)))

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
          privacy = ~js.boolean("privacy")
        yield
          val ok = !disposable
          logger.info:
            s"VerifyMail $domain = $ok {block:$block,disposable:$disposable,privacy:$privacy}"
          ok
        ).getOrElse:
          throw lila.core.lilaism.LilaException(s"$url ${res.status} ${res.body[String].take(200)}")
      .monTry(res => lila.mon.security.verifyMailApi.fetch(res.isSuccess, res.getOrElse(true)))
