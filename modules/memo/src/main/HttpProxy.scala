package lila.memo

import play.api.libs.ws.*
import com.softwaremill.tagging.*
import scalalib.Iso
import reactivemongo.api.bson.BSONHandler

import lila.memo.SettingStore
import lila.memo.SettingStore.{ StringReader, Formable }
import lila.core.config.Secret

trait ProxyCredentials
trait ProxyHostPort

final class HttpProxy(settingStore: SettingStore.Builder):

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

  private val credentialsIso = Iso.string[CredOption](Credentials.read, _.so(_.show))
  private given BSONHandler[CredOption] = lila.db.dsl.isoHandler(using credentialsIso)
  private given StringReader[CredOption] = StringReader.fromIso(using credentialsIso)
  private val hostPortIso = Iso.string[HostOption](HostPort.read, _.so(_.show))
  private given BSONHandler[HostOption] = lila.db.dsl.isoHandler(using hostPortIso)
  private given StringReader[HostOption] = StringReader.fromIso(using hostPortIso)
  private given Formable[CredOption] = stringPair(using credentialsIso)
  private given Formable[HostOption] = stringPair(using hostPortIso)
  private def stringPair[A](using iso: Iso.StringIso[A]): Formable[A] = Formable[A]: v =>
    import play.api.data.Form
    import play.api.data.Forms.*
    Form(
      single("v" -> text.verifying(t => t.isEmpty || t.count(_ == ':') == 1))
    ).fill(iso.to(v))

  val credentials = settingStore[Option[Credentials]](
    "relayProxyCredentials",
    default = none,
    text =
      "Broadcast: proxy credentials to fetch from external sources. Leave empty to use no auth (?!). Format: username:password".some
  ).taggedWith[ProxyCredentials]

  val hostPort = settingStore[Option[HostPort]](
    "relayProxyHostPort",
    default = none,
    text =
      "Broadcast: proxy host and port to fetch from external sources. Leave empty to use no proxy. Format: host:port".some
  ).taggedWith[ProxyHostPort]

  def select(): Option[DefaultWSProxyServer] =
    for
      hostPort <- hostPort.get()
      creds = credentials.get()
    yield DefaultWSProxyServer(
      host = hostPort.host,
      port = hostPort.port,
      principal = creds.map(_.user),
      password = creds.map(_.password.value)
    )
