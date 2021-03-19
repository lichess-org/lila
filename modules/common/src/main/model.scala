package lila.common

import scala.concurrent.duration._
import io.lemonlabs.uri.{ IpV4, IpV6 }

case class ApiVersion(value: Int) extends AnyVal with IntValue with Ordered[ApiVersion] {
  def compare(other: ApiVersion) = Integer.compare(value, other.value)
  def gt(other: Int)             = value > other
  def gte(other: Int)            = value >= other
  def puzzleV2                   = value >= 6
}

case class AssetVersion(value: String) extends AnyVal with StringValue

object AssetVersion {
  var current = random
  def change() = { current = random }
  private def random = AssetVersion(ornicar.scalalib.Random secureString 6)
}

case class IsMobile(value: Boolean) extends AnyVal with BooleanValue

sealed trait IpAddress {
  protected def unspecified: Boolean
  protected def loopback: Boolean
  def blockable = !unspecified && !loopback
  def value: String
  override def toString = value
}
case class IpV4Address(a: Byte, b: Byte, c: Byte, d: Byte) extends IpAddress {
  def unspecified = a == 0 && b == 0 && c == 0 && d == 0
  def loopback    = a == 127 && b == 0 && c == 0 && d == 1
  def value       = IpV4(a, b, c, d).value
}
case class IpV6Address(a: Char, b: Char, c: Char, d: Char, e: Char, f: Char, g: Char, h: Char)
    extends IpAddress {
  def unspecified = a == 0 && b == 0 && c == 0 && d == 0 && e == 0 && e == 0 && f == 0 && g == 0 && h == 0
  def loopback    = a == 0 && b == 0 && c == 0 && d == 0 && e == 0 && e == 0 && f == 0 && g == 0 && h == 1
  def value       = IpV6(a, b, c, d, e, f, g, h).value.stripPrefix("[").stripSuffix("]")
}

object IpAddress {
  def from(str: String): Option[IpAddress] =
    IpV4.parseOption(str).map(ip => IpV4Address(ip.octet1, ip.octet2, ip.octet3, ip.octet4)) orElse IpV6
      .parseOption(f"[$str]")
      .map(ip =>
        IpV6Address(ip.piece1, ip.piece2, ip.piece3, ip.piece4, ip.piece5, ip.piece6, ip.piece7, ip.piece8)
      )
  def unchecked(str: String): IpAddress = from(str).get
}

case class NormalizedEmailAddress(value: String) extends AnyVal with StringValue

case class EmailAddress(value: String) extends AnyVal with StringValue {
  def conceal =
    value split '@' match {
      case Array(user, domain) => s"${user take 3}*****@$domain"
      case _                   => value
    }

  def normalize =
    NormalizedEmailAddress {
      // changing normalization requires database migration!
      val lower = value.toLowerCase
      lower.split('@') match {
        case Array(name, domain) if EmailAddress.gmailLikeNormalizedDomains(domain) =>
          val normalizedName = name
            .replace(".", "")  // remove all dots
            .takeWhile('+' !=) // skip everything after the first '+'
          if (normalizedName.isEmpty) lower else s"$normalizedName@$domain"
        case _ => lower
      }
    }

  def domain: Option[Domain] =
    value split '@' match {
      case Array(_, domain) => Domain from domain.toLowerCase
      case _                => none
    }

  def similarTo(other: EmailAddress) = normalize == other.normalize

  def isNoReply  = EmailAddress isNoReply value
  def isSendable = !isNoReply

  // safer logs
  override def toString = "EmailAddress(****)"
}

object EmailAddress {

  private val regex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  // adding normalized domains requires database migration!
  private val gmailLikeNormalizedDomains =
    Set("gmail.com", "googlemail.com", "protonmail.com", "protonmail.ch", "pm.me")

  def isValid(str: String) =
    str.sizeIs < 320 &&
      regex.matches(str) && !str.contains("..") && !str.contains(".@") && !str.startsWith(".")

  def from(str: String): Option[EmailAddress] =
    isValid(str) option EmailAddress(str)

  private def isNoReply(str: String) = str.startsWith("noreply.") && str.endsWith("@lichess.org")
}

case class Domain private (value: String) extends AnyVal with StringValue {
  // heuristic to remove user controlled subdomain tails:
  // tail.domain.com, tail.domain.co.uk, tail.domain.edu.au, etc.
  def withoutSubdomain: Option[Domain] =
    value.split('.').toList.reverse match {
      case tld :: sld :: tail :: _ if sld.lengthIs <= 3 => Domain from s"$tail.$sld.$tld"
      case tld :: sld :: _                              => Domain from s"$sld.$tld"
      case _                                            => none
    }
  def lower = Domain.Lower(value.toLowerCase)
}

object Domain {
  // https://stackoverflow.com/a/26987741/1744715
  private val regex =
    """^(((?!-))(xn--|_{1,1})?[a-z0-9-]{0,61}[a-z0-9]{1,1}\.)*(xn--)?([a-z0-9][a-z0-9\-]{0,60}|[a-z0-9-]{1,30}\.[a-z]{2,})$""".r
  def isValid(str: String)              = regex.matches(str)
  def from(str: String): Option[Domain] = isValid(str) option Domain(str)
  def unsafe(str: String): Domain       = Domain(str)

  case class Lower(value: String) extends AnyVal with StringValue {
    def domain = Domain(value)
  }
}

case class Strings(value: List[String]) extends AnyVal
case class UserIds(value: List[String]) extends AnyVal
case class Ints(value: List[Int])       extends AnyVal

case class Every(value: FiniteDuration)  extends AnyVal
case class AtMost(value: FiniteDuration) extends AnyVal

case class Template(value: String) extends AnyVal
