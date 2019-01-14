package lila.security

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lila.base.LilaException
import lila.common.{ Chronometer, Domain }

private final class DnsApi(
    baseUrl: String,
    fetchTimeout: FiniteDuration
)(implicit system: akka.actor.ActorSystem) {

  // only valid email domains that are not whitelisted should make it here
  def mx(domain: Domain): Fu[List[Domain]] = failsafe(domain, List(domain)) {
    mxCache get domain
  }

  // only valid email domains that are not whitelisted should make it here
  def a(domain: Domain): Fu[Boolean] = failsafe(domain, true) {
    aCache get domain
  }

  private val mxCache: AsyncLoadingCache[Domain, List[Domain]] = Scaffeine()
    .expireAfterWrite(2 days)
    .buildAsyncFuture(domain => {
      lila.mon.security.dnsApi.mx.count()
      fetch(s"mx/$domain") {
        _ flatMap { obj =>
          (obj \ "value").asOpt[String].map(_ split '\t') collect {
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

  private val aCache: AsyncLoadingCache[Domain, Boolean] = Scaffeine()
    .expireAfterWrite(2 days)
    .buildAsyncFuture(domain => {
      lila.mon.security.dnsApi.a.count()
      fetch(s"a/$domain") { _.nonEmpty }
    }.mon(_.security.dnsApi.a.time) addFailureEffect { _ =>
      lila.mon.security.dnsApi.a.error()
    })

  private def fetch[A](path: String)(f: List[JsObject] => A): Fu[A] =
    WS.url(s"$baseUrl/$path").get withTimeout fetchTimeout map {
      case res if res.status == 200 || res.status == 404 => res.json.asOpt[List[JsObject]] match {
        case Some(objs) => f(objs)
        case _ => throw LilaException(res.body take 100)
      }
      case res => throw LilaException(s"Status ${res.status}")
    }

  // if the DNS service fails, assume the best
  private def failsafe[A](domain: Domain, default: => A)(f: => Fu[A]): Fu[A] = f recover {
    case e: Exception =>
      logger.warn(s"DnsApi $domain", e)
      default
  }
}
