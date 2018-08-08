package lidraughts.security

import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.duration._

import lidraughts.common.IpAddress

final class IpIntel(asyncCache: lidraughts.memo.AsyncCache.Builder, lidraughtsEmail: String) {

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
      val url = s"http://check.getipintel.net/check.php?ip=$ip&contact=$lidraughtsEmail"
      WS.url(url).get().map(_.body).mon(_.security.proxy.request.time).flatMap { str =>
        parseFloatOption(str).fold[Fu[Int]](fufail(s"Invalid ratio ${str.take(140)}")) { ratio =>
          if (ratio < 0) fufail(s"Error code $ratio")
          else fuccess((ratio * 100).toInt)
        }
      }.addEffects(
        fail = _ => lidraughts.mon.security.proxy.request.failure(),
        succ = percent => {
          lidraughts.mon.security.proxy.percent(percent max 0)
          lidraughts.mon.security.proxy.request.success()
        }
      )
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
