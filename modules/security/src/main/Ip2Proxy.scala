package lila.security

import com.github.blemale.scaffeine.AsyncLoadingCache
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.IpAddress

final class Ip2Proxy(
    ws: WSClient,
    cacheApi: lila.memo.CacheApi,
    checkUrl: String
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  def apply(ip: IpAddress): Fu[Boolean] =
    cache.get(ip).recover { case e: Exception =>
      logger.warn(s"Ip2Proxy $ip", e)
      false
    }

  def keepProxies(ips: Seq[IpAddress]): Fu[Set[IpAddress]] =
    batch(ips)
      .map {
        _.view
          .zip(ips)
          .collect { case (true, ip) =>
            ip
          }
          .toSet
      }
      .recover { case e: Exception =>
        logger.warn(s"Ip2Proxy $ips", e)
        Set.empty
      }

  private def batch(@scala.annotation.unused ips: Seq[IpAddress]): Fu[Seq[Boolean]] = fuccess(
    Seq.empty[Boolean]
  )
  // ips.take(50) match { // 50 * ipv6 length < max url length
  //   case Nil      => fuccess(Seq.empty[Boolean])
  //   case List(ip) => apply(ip).dmap(Seq(_))
  //   case ips =>
  //     ips.flatMap(cache.getIfPresent).sequenceFu flatMap { cached =>
  //       if (cached.size == ips.size) fuccess(cached)
  //       else
  //         ws.url(s"$checkUrl/batch")
  //           .addQueryStringParameters("ips" -> ips.mkString(","))
  //           .get()
  //           .withTimeout(3 seconds)
  //           .map {
  //             _.json.asOpt[Seq[JsObject]] ?? {
  //               _.map(readIsProxy)
  //             }
  //           }
  //           .flatMap { res =>
  //             if (res.size == ips.size) fuccess(res)
  //             else fufail(s"Ip2Proxy missing results for $ips -> $res")
  //           }
  //           .addEffect {
  //             _.zip(ips) foreach {
  //               case (proxy, ip) => cache.put(ip, fuccess(proxy))
  //             }
  //           }
  //           .monSuccess(_.security.proxy.request)
  //     }
  // }

  private val cache: AsyncLoadingCache[IpAddress, Boolean] = cacheApi.scaffeine
    .expireAfterWrite(1 days)
    .buildAsyncFuture { ip =>
      checkUrl.nonEmpty ?? ws
        .url(checkUrl)
        .addQueryStringParameters("ip" -> ip.value)
        .get()
        .withTimeout(2 seconds)
        .dmap(_.json)
        .dmap(readIsProxy)
        .monSuccess(_.security.proxy.request)
    }

  private def readIsProxy(js: JsValue): Boolean =
    (js \ "proxy_type").asOpt[String].exists("-" !=)
}
