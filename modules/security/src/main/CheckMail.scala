package lila.security

import play.api.libs.json._
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.api.ReadPreference
import play.api.libs.ws.JsonBodyReadables._
import scala.concurrent.duration._

import lila.common.Domain
import lila.db.dsl._

/* An expensive API detecting disposable email.
 * Only hit after trying everything else (DnsApi)
 * and save the result forever. */
final private class CheckMail(
    ws: StandaloneWSClient,
    config: SecurityConfig.CheckMail,
    mongoCache: lila.memo.MongoCache.Api
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  def apply(domain: Domain.Lower): Fu[Boolean] =
    if (config.key.value.isEmpty) fuccess(true)
    else
      cache
        .get(domain)
        .withTimeoutDefault(2.seconds, true)
        .recover {
          case e: Exception =>
            logger.warn(s"CheckMail $domain ${e.getMessage}", e)
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
        ReadPreference.secondaryPreferred
      ) map { ids =>
      val dropSize = prefix.length + 1
      ids.map(_ drop dropSize)
    }

  private val prefix = "security:check_mail"

  private val cache = mongoCache[Domain.Lower, Boolean](
    512,
    prefix,
    1000 days,
    _.toString
  ) { loader =>
    _.maximumSize(512)
      .buildAsyncFuture(loader(fetch))
  }

  private def fetch(domain: Domain.Lower): Fu[Boolean] =
    ws.url(config.url)
      .withQueryStringParameters("domain" -> domain.value, "disable_test_connection" -> "true")
      .withHttpHeaders("x-rapidapi-key" -> config.key.value)
      .get()
      .withTimeout(15.seconds)
      .map {
        case res if res.status == 200 =>
          val readBool   = readRandomBoolean(res.body[JsValue]) _
          val valid      = readBool("valid")
          val block      = readBool("block")
          val disposable = readBool("disposable")
          val reason     = ~(res.body[JsValue] \ "reason").asOpt[String]
          val ok         = valid && !block && !disposable
          logger.info(s"CheckMail $domain = $ok ($reason) {valid:$valid,block:$block,disposable:$disposable}")
          ok
        case res =>
          throw lila.base.LilaException(s"${config.url} $domain ${res.status} ${res.body take 200}")
      }
      .monTry(res => _.security.checkMailApi.fetch(res.isSuccess, res.getOrElse(true)))

  // sometimes it's "1" and sometimes it's "true"
  // and we're paying for that shit
  private def readRandomBoolean(js: JsValue)(key: String) =
    ~ {
      (js \ key).asOpt[Boolean] orElse
        (js \ key).asOpt[Int].map(1.==) orElse
        (js \ key).asOpt[String].map("1".==)
    }
}
