package lila.common

import io.mola.galimatias.IPv4Address.parseIPv4Address
import io.mola.galimatias.IPv6Address.parseIPv6Address
import scala.util.Try
import scala.concurrent.duration._

case class ApiVersion(value: Int) extends AnyVal with IntValue with Ordered[ApiVersion] {
  def compare(other: ApiVersion) = Integer.compare(value, other.value)
  def gt(other: Int)             = value > other
  def gte(other: Int)            = value >= other
  def puzzleV2                   = value >= 6
}

case class AssetVersion(value: String) extends AnyVal with StringValue

object AssetVersion {
  var current        = random
  def change()       = { current = random }
  private def random = AssetVersion(SecureRandom nextString 6)
}

case class IsMobile(value: Boolean) extends AnyVal with BooleanValue

case class Bearer(secret: String) extends AnyVal {
  override def toString = "Bearer(***)"
}
object Bearer {
  def random()         = Bearer(s"lio_${SecureRandom.nextString(32)}")
  def randomPersonal() = Bearer(s"lip_${SecureRandom.nextString(20)}")
}

sealed trait IpAddress {
  def value: String
  override def toString = value
}
case class IpV4Address(value: String) extends IpAddress
case class IpV6Address(value: String) extends IpAddress

object IpAddress {
  private def parse(str: String): Try[IpAddress] = Try {
    if (str.contains(".")) IpV4Address(parseIPv4Address(str).toString)
    else IpV6Address(parseIPv6Address(str).toString)
  }
  def from(str: String): Option[IpAddress] = parse(str).toOption
  def unchecked(str: String): IpAddress    = parse(str).get
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

  val clasIdRegex = """^noreply\.class\.(\w{8})\.[\w-]+@lichess\.org""".r
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

case class Preload[A](value: Option[A]) extends AnyVal {
  def orLoad(f: => Fu[A]): Fu[A] = value.fold(f)(fuccess)
}
object Preload {
  def apply[A](value: A): Preload[A] = Preload(value.some)
  def none[A]                        = Preload[A](None)
}
