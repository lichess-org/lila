package lila.security

import play.api.mvc.RequestHeader
import play.api.data.Form

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, Me }

case class Dated[V](value: V, date: Instant) extends Ordered[Dated[V]]:
  def compare(other: Dated[V]) = other.date compareTo date
  def map[X](f: V => X)        = copy(value = f(value))
  def seconds                  = date.toSeconds

case class AuthInfo(user: UserId, hasFp: Boolean)

case class FingerPrintedUser(me: Me, hasFingerPrint: Boolean)

case class AppealUser(me: Me)

case class UserSession(
    _id: String,
    ip: IpAddress,
    ua: UserAgent,
    api: Option[Int],
    date: Option[Instant]
):
  inline def id = _id
  def isMobile  = api.isDefined || Mobile.LichessMobileUaTrim.is(ua)

case class LocatedSession(session: UserSession, location: Option[Location])

case class IpAndFp(ip: IpAddress, fp: Option[String], user: UserId)

case class HcaptchaPublicConfig(key: String, enabled: Boolean)

case class HcaptchaForm[A](form: Form[A], config: HcaptchaPublicConfig, skip: Boolean):
  def enabled                    = config.enabled && !skip
  def apply(key: String)         = form(key)
  def withForm[B](form: Form[B]) = HcaptchaForm(form, config, skip)

case class LameNameCheck(value: Boolean) extends AnyVal

case class UserSignup(
    user: User,
    email: EmailAddress,
    req: RequestHeader,
    fingerPrint: Option[FingerHash],
    suspIp: Boolean
)

enum UserClient:
  case PC, Mob, App
object UserClient:
  def apply(ua: UserAgent): UserClient =
    if ua.value.contains("Lichobile") || Mobile.LichessMobileUaTrim.is(ua) then UserClient.App
    else if ua.value.contains("Mobile") then UserClient.Mob
    else UserClient.PC
