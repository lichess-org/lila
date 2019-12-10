package lila.security

import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.{ IpAddress, EmailAddress }

final class IpIntel(
    ws: WSClient,
    asyncCache: lila.memo.AsyncCache.Builder,
    contactEmail: EmailAddress
) {

  def apply(ip: IpAddress): Fu[Int] = failable(ip) recover {
    case e: Exception =>
      logger.warn(s"IpIntel $ip", e)
      0
  }

  def failable(ip: IpAddress): Fu[Int] =
    if (IpIntel isBlacklisted ip) fuccess(90)
    else cache get ip

  private val cache = asyncCache.multi[IpAddress, Int](
    name = "ipIntel",
    f = ip => {
      val url = s"https://check.getipintel.net/check.php?ip=$ip&contact=${contactEmail.value}"
      ws.url(url)
        .get()
        .dmap(_.body)
        .flatMap { str =>
          str.toFloatOption.fold[Fu[Int]](fufail(s"Invalid ratio ${str.take(140)}")) { ratio =>
            if (ratio < 0) fufail(s"IpIntel error $ratio on $url")
            else fuccess((ratio * 100).toInt)
          }
        }
        .monSuccess(_.security.proxy.request)
        .addEffect { percent =>
          lila.mon.security.proxy.percent.record(percent max 0)
        }
    },
    expireAfter = _.ExpireAfterAccess(3 days)
  )
}

object IpIntel {

  // Proxies ipintel doesn't detect
  private val blackList = List(
    "5.121.",
    "5.122."
  )

  def isBlacklisted(ip: IpAddress): Boolean = blackList.exists(ip.value.startsWith)
}
