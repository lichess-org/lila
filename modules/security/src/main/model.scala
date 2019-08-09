package lidraughts.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lidraughts.user.User
import lidraughts.common.{ IpAddress, EmailAddress }

case class FingerPrintedUser(user: User, fp: Option[FingerHash]) {
  def hasFingerPrint = fp.isDefined
}

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

case class Signup(user: User, email: EmailAddress, req: RequestHeader, fingerPrint: Option[FingerHash])
