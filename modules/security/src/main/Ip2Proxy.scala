package lila.security

import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.IpAddress

trait Ip2Proxy {

  def apply(ip: IpAddress): Fu[IsProxy]

  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]]
}

case class IsProxy(name: Option[String]) extends AnyVal {
  def is = name.isDefined
}

final class Ip2ProxySkip extends Ip2Proxy {

  def apply(ip: IpAddress): Fu[IsProxy] = fuccess(IsProxy(none))

  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] = fuccess(Map.empty)
}

final class Ip2ProxyServer(
    ws: StandaloneWSClient,
    cacheApi: lila.memo.CacheApi,
    checkUrl: String
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) extends Ip2Proxy {

  def apply(ip: IpAddress): Fu[IsProxy] =
    cache.get(ip).recover { case e: Exception =>
      logger.warn(s"Ip2Proxy $ip", e)
      IsProxy(none)
    }

  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] =
    batch(ips)
      .map {
        _.view
          .zip(ips)
          .collect { case (IsProxy(Some(name)), ip) => ip -> name }
          .toMap
      }
      .recover { case e: Exception =>
        logger.warn(s"Ip2Proxy $ips", e)
        Map.empty
      }

  private def batch(ips: Seq[IpAddress]): Fu[Seq[IsProxy]] =
    ips.distinct.take(50) match { // 50 * ipv6 length < max url length
      case Nil      => fuccess(Seq.empty[IsProxy])
      case List(ip) => apply(ip).dmap(Seq(_))
      case ips =>
        ips.flatMap(cache.getIfPresent).sequenceFu flatMap { cached =>
          if (cached.sizeIs == ips.size) fuccess(cached)
          else
            ws.url(s"$checkUrl/batch")
              .addQueryStringParameters("ips" -> ips.mkString(","))
              .get()
              .withTimeout(3 seconds)
              .map {
                _.body[JsValue].asOpt[Seq[JsObject]] ?? {
                  _.map(readProxyName)
                }
              }
              .flatMap { res =>
                if (res.sizeIs == ips.size) fuccess(res)
                else fufail(s"Ip2Proxy missing results for $ips -> $res")
              }
              .addEffect {
                _.zip(ips) foreach { case (proxy, ip) =>
                  cache.put(ip, fuccess(proxy))
                  lila.mon.security.proxy.result(proxy.name).increment().unit
                }
              }
        }
    }

  private val cache: AsyncLoadingCache[IpAddress, IsProxy] = cacheApi.scaffeine
    .expireAfterWrite(1 days)
    .buildAsyncFuture { ip =>
      ws
        .url(checkUrl)
        .addQueryStringParameters("ip" -> ip.value)
        .get()
        .withTimeout(2 seconds)
        .dmap(_.body[JsValue])
        .dmap(readProxyName)
        .monSuccess(_.security.proxy.request)
        .addEffect { result =>
          lila.mon.security.proxy.result(result.name).increment().unit
        }
    }

  private def readProxyName(js: JsValue): IsProxy = IsProxy {
    for {
      tpe <- (js \ "proxy_type").asOpt[String]
      if tpe != "-"
      country = (js \ "country_short").asOpt[String]
    } yield s"$tpe:${country | "?"}"
  }
}
