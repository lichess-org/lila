package lila.core

import io.mola.galimatias.IPv4Address.parseIPv4Address
import io.mola.galimatias.IPv6Address.parseIPv6Address
import scalalib.SecureRandom

import java.net.InetAddress
import scala.util.Try

import lila.core.socket.Sri
import lila.core.userId.UserId

object net:

  sealed trait IpAddress:
    def value: String
    def inet: Option[InetAddress]
    def str: IpAddressStr = IpAddressStr(value)
    override def toString = value

  case class IpV4Address(value: String) extends IpAddress:
    def inet = Try(InetAddress.getByAddress(value.split('.').map(_.toInt.toByte))).toOption
  case class IpV6Address(value: String) extends IpAddress:
    def inet = Try(parseIPv6Address(value).toInetAddress).toOption

  object IpAddress:
    private def parse(str: String): Try[IpAddress] = Try:
      if str.contains(".") then IpV4Address(parseIPv4Address(str).toString)
      else IpV6Address(parseIPv6Address(str).toString)
    def from(str: String): Option[IpAddress] = parse(str).toOption
    def unchecked(str: String): IpAddress    = parse(str).get

  opaque type IpAddressStr = String
  object IpAddressStr extends OpaqueString[IpAddressStr]

  opaque type Domain = String
  object Domain extends OpaqueString[Domain]:
    extension (a: Domain) def lower = Domain.Lower(a.value.toLowerCase)

    // https://stackoverflow.com/a/26987741/1744715
    private val regex                     = """(?i)^_?[a-z0-9-]{1,63}+(?:\._?[a-z0-9-]{1,63}+)*$""".r
    def isValid(str: String)              = str.contains('.') && regex.matches(str)
    def from(str: String): Option[Domain] = isValid(str).option(Domain(str))
    def unsafe(str: String): Domain       = Domain(str)

    opaque type Lower = String
    object Lower extends OpaqueString[Lower]

  opaque type UserAgent = String
  object UserAgent extends OpaqueString[UserAgent]

  opaque type Crawler = Boolean
  object Crawler extends YesNo[Crawler]

  case class LichessMobileUa(
      version: String,
      userId: Option[UserId],
      sri: Sri,
      osName: String,
      osVersion: String,
      device: String
  )

  opaque type ApiVersion = Int
  object ApiVersion extends RichOpaqueInt[ApiVersion]:
    def puzzleV2(v: ApiVersion) = v >= 6
    val lichobile: ApiVersion   = 6
    val mobile: ApiVersion      = 10 // i.e. github.com/lichess-org/mobile

  opaque type AssetVersion = String
  object AssetVersion extends OpaqueString[AssetVersion]:
    private var stored = random
    def current        = stored
    def change()       =
      stored = random
      current

    private def random = AssetVersion(SecureRandom.nextString(6))
    case class Changed(version: AssetVersion)

  opaque type Bearer = String
  object Bearer extends OpaqueString[Bearer]:
    def random()         = Bearer(s"lio_${SecureRandom.nextString(32)}")
    def randomPersonal() = Bearer(s"lip_${SecureRandom.nextString(20)}")
