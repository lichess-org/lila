package lila.relay

import scala.util.matching.Regex
import io.mola.galimatias.URL
import play.api.libs.ws.*
import com.softwaremill.tagging.*
import scalalib.Iso
import reactivemongo.api.bson.BSONHandler

import lila.memo.SettingStore
import lila.memo.SettingStore.{ StringReader, Formable }
import lila.memo.SettingStore.Formable.given
import lila.core.config.Secret

private opaque type CanProxy = Boolean
private object CanProxy extends YesNo[CanProxy]

private type ProxySelector = URL => CanProxy ?=> Option[DefaultWSProxyServer]

final class RelayProxy(settingStore: SettingStore.Builder):

  case class Credentials(user: String, password: Secret):
    def show = s"$user:${password.value}"
  private object Credentials:
    def read(str: String): Option[Credentials] = str.split(":") match
      case Array(user, password) => Credentials(user, Secret(password)).some
      case _ => none

  case class HostPort(host: String, port: Int):
    def show = s"$host:$port"
  private object HostPort:
    def read(str: String): Option[HostPort] = str.split(":") match
      case Array(host, port) => port.toIntOption.map(HostPort(host, _))
      case _ => none

  private type CredOption = Option[Credentials]
  private type HostOption = Option[HostPort]

  private object CredentialsOption:
    val credentialsIso = Iso.string[CredOption](Credentials.read, _.so(_.show))
    given BSONHandler[CredOption] = lila.db.dsl.isoHandler(using credentialsIso)
    given StringReader[CredOption] = StringReader.fromIso(using credentialsIso)
  private object HostPortOption:
    val hostPortIso = Iso.string[HostOption](HostPort.read, _.so(_.show))
    given BSONHandler[HostOption] = lila.db.dsl.isoHandler(using hostPortIso)
    given StringReader[HostOption] = StringReader.fromIso(using hostPortIso)

  private given Formable[CredOption] = stringPair(using CredentialsOption.credentialsIso)
  private given Formable[HostOption] = stringPair(using HostPortOption.hostPortIso)
  private def stringPair[A](using iso: Iso.StringIso[A]): Formable[A] = Formable[A]: v =>
    import play.api.data.Form
    import play.api.data.Forms.*
    Form(
      single("v" -> text.verifying(t => t.isEmpty || t.count(_ == ':') == 1))
    ).fill(iso.to(v))

  import CredentialsOption.given
  val credentials = settingStore[Option[Credentials]](
    "relayProxyCredentials",
    default = none,
    text =
      "Broadcast: proxy credentials to fetch from external sources. Leave empty to use no auth (?!). Format: username:password".some
  ).taggedWith[ProxyCredentials]

  import HostPortOption.given
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
