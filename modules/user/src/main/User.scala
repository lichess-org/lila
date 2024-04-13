package lila.user

import chess.PlayerTitle
import play.api.i18n.Lang
import java.time.Duration

import lila.core.email.NormalizedEmailAddress
import lila.core.LightUser
import lila.core.user.{
  LightPerf,
  UserMark,
  UserMarks,
  UserEnabled,
  Emails,
  WithPerf,
  Plan,
  PlayTime,
  Profile,
  TotpSecret,
  Count
}
import lila.core.i18n.Language
import lila.rating.PerfType
import lila.core.perf.{ Perf, UserPerfs, UserWithPerfs }

object UserExt:
  extension (u: User) def userLanguage: Option[Language] = u.realLang.map(Language.apply)

type CredentialCheck = ClearPassword => Boolean
case class LoginCandidate(user: User, check: CredentialCheck, isBlanked: Boolean, must2fa: Boolean = false):
  import LoginCandidate.*
  import lila.user.TotpSecret.verify
  def apply(p: PasswordAndToken): Result =
    val res =
      if user.totpSecret.isEmpty && must2fa then Result.Must2fa
      else if check(p.password) then
        user.totpSecret.fold[Result](Result.Success(user)): tp =>
          p.token.fold[Result](Result.MissingTotpToken): token =>
            if tp.verify(token) then Result.Success(user) else Result.InvalidTotpToken
      else if isBlanked then Result.BlankedPassword
      else Result.InvalidUsernameOrPassword
    lila.mon.user.auth.count(res.success).increment()
    res
  def option(p: PasswordAndToken): Option[User] = apply(p).toOption
object LoginCandidate:
  enum Result(val toOption: Option[User]):
    def success = toOption.isDefined
    case Success(user: User)       extends Result(user.some)
    case InvalidUsernameOrPassword extends Result(none)
    case Must2fa                   extends Result(none)
    case BlankedPassword           extends Result(none)
    case WeakPassword              extends Result(none)
    case MissingTotpToken          extends Result(none)
    case InvalidTotpToken          extends Result(none)

opaque type Erased = Boolean
object Erased extends YesNo[Erased]

case class WithPerfsAndEmails(user: UserWithPerfs, emails: Emails)

case class ClearPassword(value: String) extends AnyVal:
  override def toString = "ClearPassword(****)"

case class TotpToken(value: String) extends AnyVal
case class PasswordAndToken(password: ClearPassword, token: Option[TotpToken])

object PlayTime:
  extension (p: PlayTime)
    def totalDuration      = Duration.ofSeconds(p.total)
    def tvDuration         = Duration.ofSeconds(p.tv)
    def nonEmptyTvDuration = Option.when(p.tv > 0)(p.tvDuration)

object nameRules:
  // what new usernames should be like -- now split into further parts for clearer error messages
  val newUsernameRegex   = "(?i)[a-z][a-z0-9_-]{0,28}[a-z0-9]".r
  val newUsernamePrefix  = "(?i)^[a-z].*".r
  val newUsernameSuffix  = "(?i).*[a-z0-9]$".r
  val newUsernameChars   = "(?i)^[a-z0-9_-]*$".r
  val newUsernameLetters = "(?i)^([a-z0-9][_-]?)+$".r
