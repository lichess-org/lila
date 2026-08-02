package lila.relay

import scala.util.matching.Regex
import io.mola.galimatias.URL
import play.api.libs.ws.*
import com.softwaremill.tagging.*

import lila.memo.HttpProxy
import lila.memo.SettingStore
import lila.memo.SettingStore.Formable.given

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
