package lila.security

import play.api.libs.json.*
import play.api.mvc.RequestHeader
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.net.IpAddress
import lila.core.security.{ Ip2ProxyApi, IsProxy }
import lila.common.HTTPRequest

final class Ip2ProxySkip extends Ip2ProxyApi:
  def ofReq(req: RequestHeader): Fu[IsProxy] = fuccess(IsProxy.empty)
  def ofIp(ip: IpAddress): Fu[IsProxy] = fuccess(IsProxy.empty)
  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] = fuccess(Map.empty)

final class Ip2ProxyServer(
    ws: StandaloneWSClient,
    cacheApi: lila.memo.CacheApi,
    checkUrl: String,
    tor: Tor
)(using Executor, Scheduler)
    extends Ip2ProxyApi:

  def ofIp(ip: IpAddress): Fu[IsProxy] =
    if tor.isExitNode(ip)
    then fuccess(IsProxy.tor)
    else cache.get(ip.value)

  def ofReq(req: RequestHeader): Fu[IsProxy] =
    ofIp(HTTPRequest.ipAddress(req)).addEffect:
      _.name.foreach: name =>
        lila.mon.security.proxy.hit(name, HTTPRequest.actionName(req)).increment()

  def getCached(ip: IpAddress): Option[Fu[IsProxy]] =
    if tor.isExitNode(ip)
    then fuccess(IsProxy.tor).some
    else cache.getIfPresent(ip.value)

  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] =
    batch(ips)
      .map:
        _.view
          .zip(ips)
          .collect { case (IsProxy(name), ip) => ip -> name }
          .toMap
      .recover { case e: Exception =>
        logger.warn(s"Ip2Proxy $ips", e)
        Map.empty
      }

  private def batch(ips: Seq[IpAddress]): Fu[Seq[IsProxy]] =
    ips.distinct.take(80) match
      case Nil => fuccess(Seq.empty[IsProxy])
      case Seq(ip) => ofIp(ip).dmap(Seq(_))
      case ips =>
        ips.flatMap(getCached).parallel.flatMap { cached =>
          if cached.sizeIs == ips.size then fuccess(cached)
          else
            // 50 * ipv6 length < max url length
            val uncachedIps = ips.filterNot(cached.contains).take(50)
            ws.url(s"$checkUrl/batch")
              .addQueryStringParameters("ips" -> uncachedIps.mkString(","))
              .get()
              .withTimeout(1.second, "Ip2Proxy.batch")
              .map:
                _.body[JsValue]
                  .asOpt[Seq[JsObject]]
                  .so:
                    _.map(readProxyName)
              .flatMap: res =>
                if res.sizeIs == uncachedIps.size then fuccess(res)
                else fufail(s"Ip2Proxy missing results for $uncachedIps -> $res")
              .addEffect:
                _.zip(uncachedIps).foreach: (proxy, ip) =>
                  cache.put(ip.value, fuccess(proxy))
                  lila.mon.security.proxy.result(proxy.name).increment()
              .map: res =>
                cached ++ res
        }

  private val cache = cacheApi[String, IsProxy](32_768, "ip2proxy.ip"):
    _.expireAfterWrite(1.hour).buildAsyncFuture: ip =>
      ws
        .url(checkUrl)
        .addQueryStringParameters("ip" -> ip)
        .get()
        .withTimeout(150.millis, "Ip2Proxy.fetch")
        .dmap(_.body[JsValue])
        .dmap(readProxyName)
        .monSuccess(_.security.proxy.request)
        .addEffect: result =>
          lila.mon.security.proxy.result(result.name).increment()
        .recoverDefault(IsProxy.empty)

  private def readProxyName(js: JsValue): IsProxy = IsProxy:
    (js \ "proxy_type").asOpt[String].filter(_ != "-") | ""
