package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import play.api.data.Form

import lila.common.{ EmailAddress, IpAddress }
import lila.user.User
import lila.common.Iso

case class Dated[V](value: V, date: DateTime) extends Ordered[Dated[V]] {
  def compare(other: Dated[V]) = other.date compareTo date
  def map[X](f: V => X)        = copy(value = f(value))
  def seconds                  = date.getSeconds
}

case class AuthInfo(user: User.ID, hasFp: Boolean)

case class FingerPrintedUser(user: User, hasFingerPrint: Boolean)

case class AppealUser(user: User)

case class UserSession(
    _id: String,
    ip: IpAddress,
    ua: String,
    api: Option[Int],
    date: Option[DateTime]
) {

  def id = _id

  def isMobile = api.isDefined
}

case class LocatedSession(session: UserSession, location: Option[Location])

case class IpAndFp(ip: IpAddress, fp: Option[String], user: User.ID)

case class HcaptchaPublicConfig(key: String, enabled: Boolean)

case class HcaptchaForm[A](form: Form[A], config: HcaptchaPublicConfig) {
  def enabled                    = config.enabled
  def apply(key: String)         = form(key)
  def withForm[B](form: Form[B]) = HcaptchaForm(form, config)
}

case class LameNameCheck(value: Boolean) extends AnyVal

case class UserSignup(
    user: User,
    email: EmailAddress,
    req: RequestHeader,
    fingerPrint: Option[FingerHash],
    suspIp: Boolean
)

case class UserAgent(value: String) {

  import UserAgent.Client

  lazy val client: Client =
    if (value contains "Lichobile") Client.App
    else if (value contains "Mobile") Client.Mob
    else Client.PC

  def parse = org.uaparser.scala.Parser.default.parse(value)
}

object UserAgent {

  implicit val userAgentIso     = Iso.string[UserAgent](UserAgent.apply, _.value)
  implicit val userAgentHandler = lila.db.BSON.isoHandler[UserAgent, String](userAgentIso)

  sealed trait Client
  object Client {
    case object PC  extends Client
    case object Mob extends Client
    case object App extends Client
  }
}
