package lila.security

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*

import lila.base.LilaException
import lila.common.Domain
import lila.db.dsl.*

final private class DnsApi(
    ws: StandaloneWSClient,
    config: SecurityConfig.DnsApi,
    mongoCache: lila.memo.MongoCache.Api
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
):

  // only valid email domains that are not whitelisted should make it here
  def mx(domain: Domain.Lower): Fu[List[Domain]] =
    failsafe(domain, List(domain.domain)) {
      mxCache get domain
    }

  given reactivemongo.api.bson.BSONHandler[Domain] = stringAnyValHandler[Domain](_.value, Domain.unsafe)

  private val mxCache = mongoCache.noHeap[Domain.Lower, List[Domain]](
    "security.mx",
    7 days,
    _.value
  ) { domain =>
    fetch(domain, "mx") {
      _ take 20 flatMap { obj =>
        (obj \ "data")
          .asOpt[String]
          .map(_ split ' ')
          .collect { case Array(_, domain) =>
            Domain.from {
              if (domain endsWith ".") domain.init
              else domain
            }
          }
          .flatten
      }
    }.monSuccess(_.security.dnsApi.mx)
  }

  private def fetch[A](domain: Domain.Lower, tpe: String)(f: List[JsObject] => A): Fu[A] =
    ws.url(config.url)
      .withQueryStringParameters("name" -> domain.value, "type" -> tpe)
      .withHttpHeaders("Accept" -> "application/dns-json")
      .get() withTimeout config.timeout map {
      case res if res.status == 200 || res.status == 404 =>
        f(~(res.body[JsValue] \ "Answer").asOpt[List[JsObject]])
      case res => throw LilaException(s"Status ${res.status}")
    }

  // if the DNS service fails, assume the best
  private def failsafe[A](domain: Domain.Lower, default: => A)(f: => Fu[A]): Fu[A] =
    f recover { case e: Exception =>
      logger.warn(s"DnsApi $domain", e)
      default
    }
