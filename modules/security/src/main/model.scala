package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lila.common.{ EmailAddress, IpAddress }
import lila.user.User

case class AuthInfo(user: User.ID, hasFp: Boolean)

case class FingerPrintedUser(user: User, hasFingerPrint: Boolean)

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

case class RecaptchaPublicConfig(key: String, enabled: Boolean)

case class LameNameCheck(value: Boolean) extends AnyVal

case class UserSignup(
    user: User,
    email: EmailAddress,
    req: RequestHeader,
    fingerPrint: Option[FingerHash],
    suspIp: Boolean
)
