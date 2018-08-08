package lidraughts.security

import org.joda.time.DateTime
import lidraughts.user.User
import lidraughts.common.IpAddress

case class FingerprintedUser(user: User, hasFingerprint: Boolean)

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
