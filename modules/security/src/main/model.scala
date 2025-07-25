package lila.security

import lila.core.id.SessionId
import lila.core.net.{ IpAddress, UserAgent }
import lila.core.misc.AtInstant

case class Dated[V](value: V, date: Instant):
  def map[X](f: V => X) = copy(value = f(value))
  def seconds = date.toSeconds

object Dated:
  given [A] => AtInstant[Dated[A]] = _.date
  given [A] => Ordering[Dated[A]] = AtInstant.atInstantOrdering

case class AuthInfo(user: UserId, hasFp: Boolean)

case class FingerPrintedUser(me: Me, hasFingerPrint: Boolean)

case class AppealUser(me: Me)

case class UserSession(
    _id: SessionId,
    ip: IpAddress,
    ua: UserAgent,
    api: Option[Int],
    date: Option[Instant]
):
  inline def id = _id
  def isMobile = api.isDefined || Mobile.LichessMobileUaTrim.is(ua)

case class LocatedSession(session: UserSession, location: Option[Location])

case class IpAndFp(ip: IpAddress, fp: Option[String], user: UserId)

case class LameNameCheck(value: Boolean) extends AnyVal

enum UserClient:
  case PC, Mob, App
object UserClient:
  def apply(ua: UserAgent): UserClient =
    if ua.value.contains("Lichobile") || Mobile.LichessMobileUaTrim.is(ua) then UserClient.App
    else if ua.value.contains("Mobile") then UserClient.Mob
    else UserClient.PC
