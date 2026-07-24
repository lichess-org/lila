package lila.relay

import scala.util.matching.Regex
import io.mola.galimatias.URL
import play.api.libs.ws.*
import com.softwaremill.tagging.*

import lila.memo.SettingStore
import lila.memo.SettingStore.Formable.given
import lila.core.config.Credentials
import lila.core.config.HostPort

private opaque type CanProxy = Boolean
private object CanProxy extends YesNo[CanProxy]

private type ProxySelector = URL => CanProxy ?=> Option[DefaultWSProxyServer]

final class RelayProxy(settingStore: SettingStore.Builder):

  import SettingStore.CredentialsOption.given
  val credentials = settingStore[Option[Credentials]](
    "relayProxyCredentials",
    default = none,
    text =
      "Broadcast: proxy credentials to fetch from external sources. Leave empty to use no auth (?!). Format: username:password".some
  ).taggedWith[ProxyCredentials]

  import SettingStore.HostPortOption.given
  val hostPort = settingStore[Option[HostPort]](
    "relayProxyHostPort",
    default = none,
    text =
      "Broadcast: proxy host and port to fetch from external sources. Leave empty to use no proxy. Format: host:port".some
  ).taggedWith[ProxyHostPort]

  import SettingStore.Regex.given
  val domainRegex = settingStore[Regex](
    "relayProxyDomainRegex",
    default = "-".r,
    text = "Broadcast: source domains that use a proxy, as a regex".some
  ).taggedWith[ProxyDomainRegex]

  private[relay] val select: ProxySelector = url =>
    allowed ?=>
      for
        hostPort <- hostPort.get()
        if allowed.yes
        proxyRegex = domainRegex.get()
        if proxyRegex.toString.nonEmpty
        if proxyRegex.unanchored.matches(url.host.toString)
        creds = credentials.get()
      yield DefaultWSProxyServer(
        host = hostPort.host,
        port = hostPort.port,
        principal = creds.map(_.user),
        password = creds.map(_.password.value)
      )
