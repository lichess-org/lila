package lila.mod

import com.github.blemale.scaffeine.Cache
import com.github.blemale.scaffeine.LoadingCache

import lila.common.IpAddress
import lila.common.SymmetricCipher
import lila.memo.CacheApi
import lila.security.Granter
import lila.user.Holder

object IpRender {

  type Raw      = String
  type Rendered = String
  type RenderIp = IpAddress => Rendered
}

final class IpRender(cipher: SymmetricCipher) {

  import IpRender._

  def apply(mod: Holder): RenderIp = if (Granter.is(_.Admin)(mod)) visible else encrypted

  val visible = (ip: IpAddress) => ip.value

  val encrypted = (ip: IpAddress) => cache.get(ip.value)

  def decrypt(str: String) = IpAddress from {
    cipher.base64UrlFriendly.decrypt(str) getOrElse str
  }

  private val cache: LoadingCache[Raw, Rendered] = CacheApi.scaffeineNoScheduler
    .maximumSize(4096)
    .build((raw: Raw) => cipher.base64UrlFriendly.encrypt(raw).get)
}
