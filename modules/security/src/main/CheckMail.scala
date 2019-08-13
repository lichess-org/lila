package lila.security

import scala.concurrent.duration._

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current

import lila.common.Domain
import lila.db.dsl._

/* An expensive API detecting disposable email.
 * Only hit after trying everything else (DnsApi)
 * and save the result forever. */
private final class CheckMail(
    url: String,
    key: String,
    mongoCache: lila.memo.MongoCache.Builder
)(implicit system: akka.actor.ActorSystem) {

  def apply(domain: Domain.Lower): Fu[Boolean] =
    if (key.isEmpty) fuccess(true)
    else cache(domain).withTimeoutDefault(2.seconds, true) recover {
      case e: Exception =>
        lila.mon.security.checkMailApi.error()
        logger.warn(s"CheckMail $domain ${e.getMessage}", e)
        true
    }

  def allBlocked: Fu[List[String]] = cache.coll.distinct[String, List](
    "_id",
    $doc(
      "_id" $regex s"^$prefix:",
      "v" -> false
    ).some
  ) map { ids =>
      val dropSize = prefix.size + 1
      ids.map(_ drop dropSize)
    }

  private val prefix = "security:check_mail"

  private val cache = mongoCache[Domain.Lower, Boolean](
    prefix = prefix,
    f = fetch,
    timeToLive = 1000 days,
    keyToString = _.toString
  )

  private def fetch(domain: Domain.Lower): Fu[Boolean] =
    WS.url(url)
      .withQueryString("domain" -> domain.value, "disable_test_connection" -> "true")
      .withHeaders("x-rapidapi-key" -> key)
      .get withTimeout 15.seconds map {
        case res if res.status == 200 =>
          val valid = ~(res.json \ "valid").asOpt[Boolean]
          val block = ~(res.json \ "block").asOpt[Boolean]
          val disposable = ~(res.json \ "disposable").asOpt[Boolean]
          val reason = ~(res.json \ "reason").asOpt[String]
          val ok = valid && !block && !disposable
          logger.info(s"CheckMail $domain = $ok ($reason)")
          lila.mon.security.checkMailApi.count()
          if (!ok) lila.mon.security.checkMailApi.block()
          ok
        case res =>
          throw lila.base.LilaException(s"$url $domain ${res.status} ${res.body take 200}")
      }
}
