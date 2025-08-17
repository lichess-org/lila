package lila.core
package security

import play.api.data.{ Form, Mapping }
import play.api.mvc.RequestHeader

import lila.core.email.EmailAddress
import lila.core.net.IpAddress
import lila.core.user.User
import lila.core.userId.{ UserId, UserName }

case class GarbageCollect(userId: UserId)
case class CloseAccount(userId: UserId)

case class ReopenAccount(user: User)

case class DeletePublicChats(userId: UserId)

trait LilaCookie:
  import play.api.mvc.*
  def cookie(
      name: String,
      value: String,
      maxAge: Option[Int] = None,
      httpOnly: Option[Boolean] = None
  ): Cookie

object LilaCookie:
  val sessionId = "sid"
  val noRemember = "noRemember"
  def sid(req: RequestHeader): Option[String] = req.session.get(sessionId)

trait SecurityApi:
  def shareAnIpOrFp(u1: UserId, u2: UserId): Fu[Boolean]
  def getUserIdsWithSameIpAndPrint(userId: UserId): Fu[Set[UserId]]

case class HcaptchaPublicConfig(key: String, enabled: Boolean)
case class HcaptchaForm[A](form: Form[A], config: HcaptchaPublicConfig, skip: Boolean):
  def enabled = config.enabled && !skip
  def apply(key: String) = form(key)
  def withForm[B](f: Form[B]) = copy(form = f)
  def fill(data: A) = copy(form = form.fill(data))

trait Hcaptcha:
  def form[A](form: Form[A])(using req: RequestHeader): Fu[HcaptchaForm[A]]

trait SignupForm:
  val emailField: Mapping[EmailAddress]
  val username: Mapping[UserName]

opaque type FingerHash = String
object FingerHash extends OpaqueString[FingerHash]

case class UserSignup(
    user: User,
    email: EmailAddress,
    req: RequestHeader,
    fingerPrint: Option[FingerHash],
    suspIp: Boolean
)

case class ClearPassword(value: String) extends AnyVal:
  override def toString = "ClearPassword(****)"

case class HashedPassword(bytes: Array[Byte])

trait Authenticator:
  def passEnc(p: ClearPassword): HashedPassword
  def setPassword(id: UserId, p: ClearPassword): Funit

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
    def is = a.value.nonEmpty
    def in(any: (IsProxy.type => IsProxy)*) = any.exists(f => f(IsProxy) == a)
    def isSafeish: Boolean = in(_.empty, _.vpn, _.privacy)
    def name = a.value.nonEmpty.option(a.value)
  def unapply(a: IsProxy): Option[String] = a.name
  // https://blog.ip2location.com/knowledge-base/what-are-the-proxy-types-supported-in-ip2proxy/
  val vpn = IsProxy("VPN") // paid VPNs (safe for users)
  val privacy = IsProxy("CPN") // consumer privacy network (akin vpn)
  val tor = IsProxy("TOR") // tor exit node
  val server = IsProxy("DCH") // servers
  val enterprise = IsProxy("EPN") // enterprise private network
  val public = IsProxy("PUB") // public proxies (unsafe for users)
  val web = IsProxy("WEB") // web proxies (garbage)
  val search = IsProxy("SES") // search engine crawlers
  val residential = IsProxy("RES") // residential proxies (suspect)
  val empty = IsProxy("")

trait Ip2ProxyApi:
  def ofReq(req: RequestHeader): Fu[IsProxy]
  def ofIp(ip: IpAddress): Fu[IsProxy]
  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]]

opaque type UserTrust = Boolean
object UserTrust extends YesNo[UserTrust]
trait UserTrustApi:
  def get(id: UserId): Fu[UserTrust]

case class AskAreRelated(users: PairOf[UserId], promise: Promise[Boolean])
