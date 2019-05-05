package lila.security

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lila.base.LilaException
import lila.common.{ Chronometer, Domain }

private final class DnsApi(
    resolverUrl: String,
    fetchTimeout: FiniteDuration
)(implicit system: akka.actor.ActorSystem) {

  // only valid email domains that are not whitelisted should make it here
  def mx(domain: Domain): Fu[List[Domain]] = failsafe(domain, List(domain)) {
    mxCache get domain
  }

  private val mxCache: AsyncLoadingCache[Domain, List[Domain]] = Scaffeine()
    .expireAfterWrite(2 days)
    .buildAsyncFuture(domain => {
      lila.mon.security.dnsApi.mx.count()
      fetch(domain, "mx") {
        _ flatMap { obj =>
          (obj \ "data").asOpt[String].map(_ split ' ') collect {
            case Array(_, domain) => Domain {
              if (domain endsWith ".") domain.init
              else domain
            }
          }
        }
      }
    }.mon(_.security.dnsApi.mx.time) addFailureEffect { _ =>
      lila.mon.security.dnsApi.mx.error()
    })

  private def fetch[A](domain: Domain, tpe: String)(f: List[JsObject] => A): Fu[A] =
    WS.url(resolverUrl)
      .withQueryString("name" -> domain.value, "type" -> tpe)
      .withHeaders("Accept" -> "application/dns-json")
      .get withTimeout fetchTimeout map {
        case res if res.status == 200 || res.status == 404 => f(~(res.json \ "Answer").asOpt[List[JsObject]])
        case res => throw LilaException(s"Status ${res.status}")
      }

  // if the DNS service fails, assume the best
  private def failsafe[A](domain: Domain, default: => A)(f: => Fu[A]): Fu[A] = f recover {
    case e: Exception =>
      logger.warn(s"DnsApi $domain", e)
      default
  }
}
