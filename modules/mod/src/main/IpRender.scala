package lila.mod

import com.github.blemale.scaffeine.LoadingCache
import scala.jdk.CollectionConverters.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.CuteNameGenerator
import lila.common.IpAddress
import lila.memo.CacheApi
import lila.security.Granter
import lila.user.Me

object IpRender:

  type Raw      = String
  type Rendered = String
  type RenderIp = IpAddress => Rendered

final class IpRender:

  import IpRender.*

  def apply(using Me): RenderIp = if Granter(_.Admin) then visible else encrypted

  private val visible = (ip: IpAddress) => ip.value

  private val encrypted = (ip: IpAddress) => cache get ip

  def decrypt(str: String): Option[IpAddress] = IpAddress.from(str) orElse
    cache.underlying.asMap.asScala.collectFirst:
      case (ip, encrypted) if encrypted == str => ip

  private val cache: LoadingCache[IpAddress, Rendered] = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(30 minutes)
    .build: (_: IpAddress) =>
      s"NoIP:${CuteNameGenerator.make(maxSize = 30).so(_.value)}-${ThreadLocalRandom.nextString(3)}"
