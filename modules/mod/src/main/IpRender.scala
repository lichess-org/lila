package lila.mod

import com.github.blemale.scaffeine.Cache
import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import lila.common.CuteNameGenerator
import lila.common.IpAddress
import lila.memo.CacheApi
import lila.security.Granter
import lila.user.Holder
import lila.common.ThreadLocalRandom

object IpRender {

  type Raw      = String
  type Rendered = String
  type RenderIp = IpAddress => Rendered
}

final class IpRender {

  import IpRender._

  def apply(mod: Holder): RenderIp = if (Granter.is(_.Admin)(mod)) visible else encrypted

  val visible = (ip: IpAddress) => ip.value

  val encrypted = (ip: IpAddress) => cache get ip

  def decrypt(str: String): Option[IpAddress] = IpAddress.from(str) orElse
    cache.underlying.asMap.asScala.collectFirst {
      case (ip, encrypted) if encrypted == str =>
        ip
    }

  private val cache: LoadingCache[IpAddress, Rendered] = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(30 minutes)
    .build((_: IpAddress) =>
      s"NoIP:${~CuteNameGenerator.make(maxSize = 30)}-${ThreadLocalRandom.nextString(3)}"
    )
}
