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

opaque type Erased = Boolean
object Erased extends YesNo[Erased]

case class WithPerfsAndEmails(user: UserWithPerfs, emails: Emails)

case class TotpToken(value: String) extends AnyVal

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
