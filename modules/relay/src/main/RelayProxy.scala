package lila.relay

import com.softwaremill.tagging.*
import io.mola.galimatias.URL
import play.api.libs.ws.*

import scala.util.matching.Regex

import lila.memo.SettingStore.Formable.given
import lila.memo.{ HttpProxy, SettingStore }

private opaque type CanProxy = Boolean
private object CanProxy extends YesNo[CanProxy]

private type ProxySelector = URL => CanProxy ?=> Option[DefaultWSProxyServer]

final class RelayProxy(proxy: HttpProxy, settingStore: SettingStore.Builder):

  import SettingStore.Regex.given
  val domainRegex = settingStore[Regex](
    "relayProxyDomainRegex",
    default = "-".r,
    text = "Broadcast: source domains that use a proxy, as a regex".some
  ).taggedWith[ProxyDomainRegex]

  private[relay] val select: ProxySelector = url =>
    allowed ?=>
      allowed.yes.so:
        for
          proxy <- proxy.select()
          proxyRegex = domainRegex.get()
          if proxyRegex.toString.nonEmpty
          if proxyRegex.unanchored.matches(url.host.toString)
        yield proxy
