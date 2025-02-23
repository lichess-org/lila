package lila.security

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.lilaism.LilaException
import lila.core.net.Domain
import lila.db.dsl.given

final private class DnsApi(
    ws: StandaloneWSClient,
    config: SecurityConfig.DnsApi,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor, Scheduler):

  // only valid email domains that are not whitelisted should make it here
  def mx(lower: Domain.Lower): Fu[List[Domain]] =
    failsafe(lower, List(lower.into(Domain))):
      mxCache.get(lower)

  private val mxCache = mongoCache.noHeap[Domain.Lower, List[Domain]](
    "security.mx",
    3.days,
    _.value
  ) { domain =>
    fetch(domain, "mx") {
      _.take(20).flatMap { obj =>
        (obj \ "data")
          .asOpt[String]
          .map(_.split(' '))
          .collect { case Array(_, domain) =>
            Domain.from:
              if domain.endsWith(".") then domain.init
              else domain
          }
          .flatten
      }
    }.monSuccess(_.security.dnsApi.mx)
  }

  private def fetch[A](domain: Domain.Lower, tpe: String)(f: List[JsObject] => A): Fu[A] =
    ws.url(config.url)
      .withQueryStringParameters("name" -> domain.value, "type" -> tpe)
      .withHttpHeaders("Accept" -> "application/dns-json")
      .get()
      .withTimeout(config.timeout, "DnsApi.fetch")
      .map:
        case res if res.status == 200 || res.status == 404 =>
          f(~(res.body[JsValue] \ "Answer").asOpt[List[JsObject]])
        case res => throw LilaException(s"Status ${res.status}")

  // if the DNS service fails, assume the best
  private def failsafe[A](domain: Domain.Lower, default: => A)(f: => Fu[A]): Fu[A] =
    f.recover { case e: Exception =>
      logger.warn(s"DnsApi $domain", e)
      default
    }
