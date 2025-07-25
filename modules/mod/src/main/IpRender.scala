package lila.mod

import com.github.blemale.scaffeine.LoadingCache
import scalalib.ThreadLocalRandom
import scalalib.cuteName.CuteNameGenerator

import scala.jdk.CollectionConverters.*

import lila.core.net.IpAddress
import lila.core.perm.Granter
import lila.memo.CacheApi

object IpRender:

  type Raw = String
  type Rendered = String
  type RenderIp = IpAddress => Rendered

final class IpRender(using Executor):

  import IpRender.*

  def apply(using Me): RenderIp = if Granter(_.Admin) then visible else encrypted

  private val visible = (ip: IpAddress) => ip.value

  private val encrypted = (ip: IpAddress) => cache.get(ip)

  def decrypt(str: String): Option[IpAddress] = IpAddress
    .from(str)
    .orElse(cache.underlying.asMap.asScala.collectFirst:
      case (ip, encrypted) if encrypted == str => ip)

  private val cache: LoadingCache[IpAddress, Rendered] = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(30.minutes)
    .build: (_: IpAddress) =>
      s"NoIP:${~CuteNameGenerator.make(maxSize = 30)}-${ThreadLocalRandom.nextString(3)}"
