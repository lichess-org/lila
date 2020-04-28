package lila.security

import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import com.github.blemale.scaffeine.AsyncCache

import lila.common.IpAddress

final class Ip2Proxy(
    ws: WSClient,
    cacheApi: lila.memo.CacheApi,
    checkUrl: String
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Ip2Proxy._

  def apply(ip: IpAddress, reason: Reason): Fu[ProxyType] = failable(ip, reason).nevermind(ProxyType("-"))

  def failable(ip: IpAddress, reason: Reason): Fu[ProxyType] =
    if (isBlacklisted(ip)) fuccess(ProxyType("BLK"))
    else cache.getFuture(ip, get(reason))

  private val cache: AsyncCache[IpAddress, ProxyType] = cacheApi.scaffeine
    .expireAfterWrite(1 days)
    .buildAsync

  private def get(reason: Reason)(ip: IpAddress): Fu[ProxyType] = {
    lila.mon.security.proxy.reason(reason.toString).increment()
    ws.url(checkUrl)
      .addQueryStringParameters("ip" -> ip.value)
      .get()
      .map { body =>
        ProxyType((body.json \ "proxy_type").asOpt[String] getOrElse "-")
      }
      .monSuccess(_.security.proxy.request)
  }
}

object Ip2Proxy {

  case class ProxyType(value: String) extends AnyVal {
    def isProxy = value != "-"
  }

  sealed trait Reason
  object Reason {
    case object GarbageCollector extends Reason
    case object UserMod          extends Reason
    case object Signup           extends Reason
  }

  // Proxies ip2proxy does not detect
  private val blackList = List(
    "5.121.",
    "5.122."
  )

  def isBlacklisted(ip: IpAddress): Boolean = blackList.exists(ip.value.startsWith)
}
