package lila.security

import org.joda.time.DateTime

case class FingerprintedUser(user: lila.user.User, hasFingerprint: Boolean)

case class UserSession(
    _id: String,
    ip: String,
    ua: String,
    api: Option[Int],
    date: Option[DateTime]) {

  def id = _id

  def isMobile = api.isDefined
}

case class LocatedSession(session: UserSession, location: Option[Location])
