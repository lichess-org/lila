package lila.core

import io.mola.galimatias.URL
import io.mola.galimatias.IPv4Address.parseIPv4Address
import io.mola.galimatias.IPv6Address.parseIPv6Address
import scalalib.SecureRandom
import scalalib.net.IpAddressStr

import java.net.InetAddress
import scala.util.Try

import lila.core.socket.Sri
import lila.core.userId.UserId
import lila.core.data.Url

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
    def unchecked(str: String): IpAddress = parse(str).get

  case class LichessMobileUa(
      version: String,
      userId: Option[UserId],
      sri: Sri,
      osName: String,
      osVersion: String,
      device: String
  )
  case class LichessMobileVersion(major: Int, minor: Int):
    def gte(maj: Int, min: Int) =
      import scala.math.Ordered.orderingToOrdered
      this >= LichessMobileVersion(maj, min)
  object LichessMobileVersion:
    val zero = LichessMobileVersion(0, 0)
    given Ordering[LichessMobileVersion] with
      def compare(x: LichessMobileVersion, y: LichessMobileVersion): Int =
        (x.major, x.minor).compare((y.major, y.minor))

  opaque type ApiVersion = Int
  object ApiVersion extends RichOpaqueInt[ApiVersion]:
    def puzzleV2(v: ApiVersion) = v >= 6
    val lichobile: ApiVersion = 6
    val mobile: ApiVersion = 10 // i.e. github.com/lichess-org/mobile

  opaque type AssetVersion = String
  object AssetVersion extends OpaqueString[AssetVersion]:
    private var stored = random
    def current = stored
    def change() =
      stored = random
      current

    private def random = AssetVersion(SecureRandom.nextString(6))
    case class Changed(version: AssetVersion)

  opaque type Origin = String
  object Origin extends OpaqueString[Origin]:
    def from(url: URL): Origin =
      // https://github.com/smola/galimatias/issues/72 will be more precise
      s"${url.scheme}://${Option(url.host).fold("")(_.toHostString)}"

  opaque type ValidReferrer = String
  object ValidReferrer extends OpaqueString[ValidReferrer]:
    import scalalib.StringOps.addQueryParam
    extension (a: ValidReferrer)
      def propagate(url: Url): Url = url.map(addQueryParam(_, "referrer", a.value))
      def propagate(call: play.api.mvc.Call): Url = propagate(Url(call.url))
