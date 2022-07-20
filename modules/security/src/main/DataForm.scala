package lila.security

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import scala.concurrent.duration._

import lila.common.{ EmailAddress, Form => LilaForm, LameName }
import lila.user.{ TotpSecret, User, UserRepo }
import User.{ ClearPassword, TotpToken }

final class DataForm(
    userRepo: UserRepo,
    val captcher: lila.hub.actors.Captcher,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator,
    lameNameCheck: LameNameCheck
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  import DataForm._

  case class Empty(gameId: String, move: String)

  val empty = Form(
    mapping(
      "gameId" -> text,
      "move"   -> text
    )(Empty.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def emptyWithCaptcha = withCaptcha(empty)

  private val anyEmail        = LilaForm.cleanNonEmptyText.verifying(Constraints.emailAddress)
  private val sendableEmail   = anyEmail.verifying(emailValidator.sendableConstraint)
  private val acceptableEmail = anyEmail.verifying(emailValidator.acceptableConstraint)
  private def acceptableUniqueEmail(forUser: Option[User]) =
    acceptableEmail.verifying(emailValidator uniqueConstraint forUser)

  private def withAcceptableDns(m: Mapping[String]) = m verifying emailValidator.withAcceptableDns

  private val preloadEmailDnsForm = Form(single("email" -> acceptableEmail))

  def preloadEmailDns(implicit req: play.api.mvc.Request[_], formBinding: FormBinding): Funit =
    preloadEmailDnsForm
      .bindFromRequest()
      .fold(
        _ => funit,
        email => emailValidator.preloadDns(EmailAddress(email))
      )

  object signup {

    val username = LilaForm.cleanNonEmptyText
      .verifying(
        Constraints minLength 2,
        Constraints maxLength 20,
        Constraints.pattern(
          regex = User.newUsernamePrefix,
          error = "usernamePrefixInvalid"
        ),
        Constraints.pattern(
          regex = User.newUsernameSuffix,
          error = "usernameSuffixInvalid"
        ),
        Constraints.pattern(
          regex = User.newUsernameChars,
          error = "usernameCharsInvalid"
        )
      )
      .verifying("usernameUnacceptable", u => !lameNameCheck.value || !LameName.username(u))
      .verifying("usernameAlreadyUsed", u => !userRepo.nameExists(u).await(3 seconds, "signupUsername"))

    private val agreementBool = boolean.verifying(b => b)

    private val agreement = mapping(
      "assistance" -> agreementBool,
      "nice"       -> agreementBool,
      "account"    -> agreementBool,
      "policy"     -> agreementBool
    )(AgreementData.apply)(AgreementData.unapply)

    val emailField = withAcceptableDns(acceptableUniqueEmail(none))

    val website = Form(
      mapping(
        "username"             -> username,
        "password"             -> text(minLength = 4),
        "email"                -> emailField,
        "agreement"            -> agreement,
        "fp"                   -> optional(nonEmptyText),
        "g-recaptcha-response" -> optional(nonEmptyText)
      )(SignupData.apply)(_ => None)
    )

    val mobile = Form(
      mapping(
        "username" -> username,
        "password" -> text(minLength = 4),
        "email"    -> emailField
      )(MobileSignupData.apply)(_ => None)
    )
  }

  val passwordReset = Form(
    mapping(
      "email"  -> sendableEmail, // allow unacceptable emails for BC
      "gameId" -> text,
      "move"   -> text
    )(PasswordReset.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def passwordResetWithCaptcha = withCaptcha(passwordReset)

  val newPassword = Form(
    single(
      "password" -> text(minLength = 4)
    )
  )

  case class PasswordResetConfirm(newPasswd1: String, newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwdReset = Form(
    mapping(
      "newPasswd1" -> nonEmptyText(minLength = 2),
      "newPasswd2" -> nonEmptyText(minLength = 2)
    )(PasswordResetConfirm.apply)(PasswordResetConfirm.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    )
  )

  val magicLink = Form(
    mapping(
      "email"  -> sendableEmail, // allow unacceptable emails for BC
      "gameId" -> text,
      "move"   -> text
    )(MagicLink.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def magicLinkWithCaptcha = withCaptcha(magicLink)

  def changeEmail(u: User, old: Option[EmailAddress]) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "passwd" -> passwordMapping(candidate),
          "email" -> withAcceptableDns {
            acceptableUniqueEmail(candidate.user.some).verifying(emailValidator differentConstraint old)
          }
        )(ChangeEmail.apply)(ChangeEmail.unapply)
      ).fill(
        ChangeEmail(
          passwd = "",
          email = old.??(_.value)
        )
      )
    }

  def setupTwoFactor(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "secret" -> nonEmptyText,
          "passwd" -> passwordMapping(candidate),
          "token"  -> nonEmptyText
        )(TwoFactor.apply)(TwoFactor.unapply).verifying(
          "invalidAuthenticationCode",
          _.tokenValid
        )
      ).fill(
        TwoFactor(
          secret = TotpSecret.random.base32,
          passwd = "",
          token = ""
        )
      )
    }

  def disableTwoFactor(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(
        tuple(
          "passwd" -> passwordMapping(candidate),
          "token" -> text.verifying("invalidAuthenticationCode", t => u.totpSecret.??(_.verify(TotpToken(t))))
        )
      )
    }

  def fixEmail(old: EmailAddress) =
    Form(
      single(
        "email" -> withAcceptableDns {
          acceptableUniqueEmail(none).verifying(emailValidator differentConstraint old.some)
        }
      )
    ).fill(old.value)

  def modEmail(user: User) = Form(single("email" -> acceptableUniqueEmail(user.some)))

  private def passwordProtected(u: User) =
    authenticator loginCandidate u map { candidate =>
      Form(single("passwd" -> passwordMapping(candidate)))
    }

  def closeAccount = passwordProtected _

  def toggleKid = passwordProtected _

  val reopen = Form(
    mapping(
      "username" -> LilaForm.cleanNonEmptyText,
      "email"    -> sendableEmail, // allow unacceptable emails for BC
      "gameId"   -> text,
      "move"     -> text
    )(Reopen.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def reopenWithCaptcha = withCaptcha(reopen)

  private def passwordMapping(candidate: User.LoginCandidate) =
    text.verifying("incorrectPassword", p => candidate.check(ClearPassword(p)))
}

object DataForm {

  case class AgreementData(
      assistance: Boolean,
      nice: Boolean,
      account: Boolean,
      policy: Boolean
  )

  case class SignupData(
      username: String,
      password: String,
      email: String,
      agreement: AgreementData,
      fp: Option[String],
      `g-recaptcha-response`: Option[String]
  ) {
    def recaptchaResponse = `g-recaptcha-response`

    def realEmail = EmailAddress(email)

    def fingerPrint = fp.filter(_.nonEmpty) map FingerPrint.apply
  }

  case class MobileSignupData(
      username: String,
      password: String,
      email: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class PasswordReset(
      email: String,
      gameId: String,
      move: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class MagicLink(
      email: String,
      gameId: String,
      move: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class Reopen(
      username: String,
      email: String,
      gameId: String,
      move: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class ChangeEmail(passwd: String, email: String) {
    def realEmail = EmailAddress(email)
  }

  case class TwoFactor(secret: String, passwd: String, token: String) {
    def tokenValid = TotpSecret(secret).verify(User.TotpToken(token))
  }
}
