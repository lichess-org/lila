package lila.relay

import scala.util.matching.Regex
import io.mola.galimatias.URL
import play.api.libs.ws.*
import com.softwaremill.tagging.*

import lila.memo.SettingStore
import lila.core.config.{ Credentials, HostPort }

private opaque type CanProxy = Boolean
private object CanProxy extends YesNo[CanProxy]

private type ProxySelector = URL => CanProxy ?=> Option[DefaultWSProxyServer]

final private class RelayProxy(
    proxyCredentials: SettingStore[Option[Credentials]] @@ ProxyCredentials,
    proxyHostPort: SettingStore[Option[HostPort]] @@ ProxyHostPort,
    proxyDomainRegex: SettingStore[Regex] @@ ProxyDomainRegex
):

  val select: ProxySelector = url =>
    allowed ?=>
      for
        hostPort <- proxyHostPort.get()
        if allowed.yes
        proxyRegex = proxyDomainRegex.get()
        if proxyRegex.toString.nonEmpty
        if proxyRegex.unanchored.matches(url.host.toString)
        creds = proxyCredentials.get()
      yield DefaultWSProxyServer(
        host = hostPort.host,
        port = hostPort.port,
        principal = creds.map(_.user),
        password = creds.map(_.password.value)
      )
