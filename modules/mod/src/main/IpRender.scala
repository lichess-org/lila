package lila.mod

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.CuteNameGenerator
import lila.common.IpAddress
import lila.memo.CacheApi
import lila.security.Granter
import lila.user.Holder

object IpRender:

  type Raw      = String
  type Rendered = String
  type RenderIp = IpAddress => Rendered

final class IpRender:

  import IpRender.*

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
      s"NoIP:${CuteNameGenerator.make(maxSize = 30).??(_.value)}-${ThreadLocalRandom.nextString(3)}"
    )
