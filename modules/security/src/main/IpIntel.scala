package lila.security

import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import com.github.blemale.scaffeine.AsyncCache

import lila.common.{ EmailAddress, IpAddress }

final class IpIntel(
    ws: WSClient,
    cacheApi: lila.memo.CacheApi,
    contactEmail: EmailAddress
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(ip: IpAddress, reason: IpIntel.Reason): Fu[Int] = failable(ip, reason).nevermind

  def failable(ip: IpAddress, reason: IpIntel.Reason): Fu[Int] =
    if (IpIntel isBlacklisted ip) fuccess(90)
    else if (contactEmail.value.isEmpty) fuccess(0)
    else cache.getFuture(ip, get(reason))

  private val cache: AsyncCache[IpAddress, Int] = cacheApi.scaffeine
    .expireAfterWrite(7 days)
    .buildAsync

  private def get(reason: IpIntel.Reason)(ip: IpAddress): Fu[Int] = {
    lila.mon.security.proxy.ipintel(reason.toString).increment()
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
  }
}

object IpIntel {

  sealed trait Reason
  object Reason {
    case object GarbageCollector extends Reason
    case object UserMod          extends Reason
    case object Signup           extends Reason
  }

  // Proxies ipintel doesn't detect
  private val blackList = List(
    "5.121.",
    "5.122."
  )

  def isBlacklisted(ip: IpAddress): Boolean = blackList.exists(ip.value.startsWith)
}
