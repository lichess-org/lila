package lila.core
package security

import lila.core.user.{ User, UserEnabled }

trait LilaCookie:
  import play.api.mvc.*
  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      RequestHeader
  ): Cookie

trait SecurityApi:
  def shareAnIpOrFp(u1: UserId, u2: UserId): Fu[Boolean]
  def getUserIdsWithSameIpAndPrint(userId: UserId): Fu[Set[UserId]]

opaque type FloodSource = String
object FloodSource extends OpaqueString[FloodSource]
trait FloodApi:
  def allowMessage(source: FloodSource, text: String): Boolean

trait SpamApi:
  def detect(text: String): Boolean
  def replace(text: String): String

trait PromotionApi:
  def test(author: User, text: String, prevText: Option[String]): Boolean
  def save(author: UserId, text: String): Unit

opaque type IsProxy = String
object IsProxy extends OpaqueString[IsProxy]:
  extension (a: IsProxy)
    def is                                  = a.value.nonEmpty
    def in(any: (IsProxy.type => IsProxy)*) = any.exists(f => f(IsProxy) == a)
    def name                                = a.value.nonEmpty.option(a.value)
  def unapply(a: IsProxy): Option[String] = a.name
  // https://blog.ip2location.com/knowledge-base/what-are-the-proxy-types-supported-in-ip2proxy/
  val vpn         = IsProxy("VPN") // paid VPNs (safe for users)
  val privacy     = IsProxy("CPN") // consumer privacy network (akin vpn)
  val tor         = IsProxy("TOR") // tor exit node
  val server      = IsProxy("DCH") // servers
  val enterprise  = IsProxy("EPN") // enterprise private network
  val public      = IsProxy("PUB") // public proxies (unsafe for users)
  val web         = IsProxy("WEB") // web proxies (garbage)
  val search      = IsProxy("SES") // search engine crawlers
  val residential = IsProxy("RES") // residential proxies (suspect)
  val empty       = IsProxy("")

trait Ip2ProxyApi:
  def apply(ip: IpAddress): Fu[IsProxy]
  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]]
