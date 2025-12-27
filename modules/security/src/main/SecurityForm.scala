package lila.security

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints
import play.api.mvc.{ Request, RequestHeader }

import lila.common.Form.*
import lila.common.{ Form as LilaForm, LameName }
import lila.core.security.ClearPassword
import lila.user.TotpSecret.{ base32, verify }
import lila.user.{ TotpSecret, TotpToken }

final class SecurityForm(
    userRepo: lila.user.UserRepo,
    authenticator: Authenticator,
    emailValidator: EmailAddressValidator,
    lameNameCheck: LameNameCheck,
    hcaptcha: Hcaptcha
)(using ec: Executor, mode: play.api.Mode):

  import SecurityForm.*

  private val newPasswordField =
    nonEmptyText(minLength = 4, maxLength = 999).verifying(PasswordCheck.newConstraint)
  private def newPasswordFieldForMe(using me: Me) =
    newPasswordField.verifying(PasswordCheck.sameConstraint(me.username.into(UserStr)))

  def myUsernameField(using me: Me) =
    LilaForm.cleanNonEmptyText
      .into[UserStr]
      .verifying("Username doesn't match the currently logged-in account.", _.is(me))

  private val anyEmail: Mapping[EmailAddress] =
    LilaForm
      .cleanNonEmptyText(minLength = 6, maxLength = EmailAddress.maxLength)
      .verifying(Constraints.emailAddress)
      .verifying("error.email", EmailAddress.isValid)
      .into[EmailAddress]

  private val sendableEmail = anyEmail.verifying(emailValidator.sendableConstraint)

  private def fullyValidEmail(using me: Option[Me]) = sendableEmail
    .verifying(emailValidator.plusConstraint)
    .verifying(emailValidator.withAcceptableDns)
    .verifying(emailValidator.uniqueConstraint(me))

  private val preloadEmailDnsForm = Form(single("email" -> sendableEmail))

  def preloadEmailDns()(using req: Request[?], formBinding: FormBinding): Funit =
    preloadEmailDnsForm
      .bindFromRequest()
      .fold(_ => funit, emailValidator.preloadDns)

  object signup extends lila.core.security.SignupForm:

    val emailField: Mapping[EmailAddress] = fullyValidEmail(using none)

    val username: Mapping[UserName] = LilaForm
      .cleanNonEmptyText(minLength = 2, maxLength = 20)
      .verifying(
        Constraints.pattern(
          regex = lila.user.nameRules.newUsernamePrefix,
          error = "usernamePrefixInvalid"
        ),
        Constraints.pattern(
          regex = lila.user.nameRules.newUsernameSuffix,
          error = "usernameSuffixInvalid"
        ),
        Constraints.pattern(
          regex = lila.user.nameRules.newUsernameChars,
          error = "usernameCharsInvalid"
        ),
        Constraints.pattern(
          regex = lila.user.nameRules.newUsernameLetters,
          error = "usernameCharsInvalid"
        )
      )
      .into[UserName]
      .verifying("usernameUnacceptable", u => !lameNameCheck.value || !LameName.username(u))
      .verifying(
        "usernameAlreadyUsed",
        u => u.id.noGhost && !userRepo.exists(u).await(3.seconds, "signupUsername")
      )

    private val agreementBool = boolean.verifying(b => b)

    private val agreement = mapping(
      "assistance" -> agreementBool,
      "nice" -> agreementBool,
      "account" -> agreementBool,
      "policy" -> agreementBool
    )(AgreementData.apply)(unapply)

    def website(using RequestHeader) = hcaptcha.form:
      Form:
        mapping(
          "username" -> username,
          "password" -> newPasswordField,
          "email" -> emailField,
          "agreement" -> agreement,
          "fp" -> optional(nonEmptyText)
        )(SignupData.apply)(_ => None)
          .verifying(PasswordCheck.errorSame, x => x.password != x.username.value)

    val mobile = Form:
      mapping(
        "username" -> username,
        "password" -> newPasswordField,
        "email" -> emailField
      )(MobileSignupData.apply)(_ => None)
        .verifying(PasswordCheck.errorSame, x => x.password != x.username.value)

  def passwordReset(using RequestHeader) = hcaptcha.form:
    Form:
      mapping(
        "email" -> sendableEmail // allow unacceptable emails for BC
      )(PasswordReset.apply)(_ => None)

  case class PasswordResetConfirm(newPasswd1: String, newPasswd2: String):
    def samePasswords = newPasswd1 == newPasswd2

  def passwdResetForMe(using me: Me) = Form(
    mapping(
      "newPasswd1" -> newPasswordFieldForMe,
      "newPasswd2" -> newPasswordFieldForMe
    )(PasswordResetConfirm.apply)(unapply)
      .verifying("newPasswordsDontMatch", _.samePasswords)
  )

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String
  ):
    def samePasswords = newPasswd1 == newPasswd2

  def passwdChange(using Executor)(using me: Me) =
    authenticator.loginCandidate.map: candidate =>
      Form:
        mapping(
          "oldPasswd" -> nonEmptyText.verifying("incorrectPassword", p => candidate.check(ClearPassword(p))),
          "newPasswd1" -> newPasswordFieldForMe,
          "newPasswd2" -> newPasswordFieldForMe
        )(Passwd.apply)(unapply)
          .verifying("newPasswordsDontMatch", _.samePasswords)

  def magicLink(using req: RequestHeader) = hcaptcha.form(
    Form(
      mapping(
        "email" -> sendableEmail // allow unacceptable emails for BC
      )(MagicLink.apply)(_ => None)
    )
  )

  def changeEmail(old: Option[EmailAddress])(using Me) =
    authenticator.loginCandidate.map: candidate =>
      Form(
        mapping(
          "passwd" -> passwordMapping(candidate),
          "email" -> fullyValidEmail.verifying(emailValidator.differentConstraint(old))
        )(ChangeEmail.apply)(unapply)
      ).fillOption(old.map { ChangeEmail("", _) })

  def setupTwoFactor(using Me) =
    authenticator.loginCandidate.map: candidate =>
      Form(
        mapping(
          "secret" -> nonEmptyText,
          "passwd" -> passwordMapping(candidate),
          "token" -> nonEmptyText
        )(TwoFactor.apply)(unapply).verifying(
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

  def disableTwoFactor(using me: Me) =
    authenticator.loginCandidate.map: candidate =>
      Form:
        tuple(
          "passwd" -> passwordMapping(candidate),
          "token" -> text.verifying(
            "invalidAuthenticationCode",
            t => me.totpSecret.so(_.verify(TotpToken(t)))
          )
        )

  def fixEmail(old: EmailAddress) =
    Form(
      single("email" -> fullyValidEmail(using none).verifying(emailValidator.differentConstraint(old.some)))
    ).fill(old)

  def modEmail(user: User) = Form(
    single("email" -> optional(anyEmail.verifying(emailValidator.uniqueConstraint(user.some))))
  )

  private def passwordProtected(using Me) =
    authenticator.loginCandidate.map: candidate =>
      Form(single("passwd" -> passwordMapping(candidate)))

  def closeAccount(using Me) =
    authenticator.loginCandidate.map: candidate =>
      Form:
        mapping(
          "username" -> myUsernameField,
          "passwd" -> passwordMapping(candidate),
          "forever" -> boolean
        )((_, _, forever) => forever)(_ => None)

  def toggleKid(using Me) = passwordProtected

  def reopen(using RequestHeader) = hcaptcha.form(
    Form:
      mapping(
        "username" -> LilaForm.cleanNonEmptyText.into[UserStr],
        "email" -> sendableEmail // allow unacceptable emails for BC
      )(Reopen.apply)(_ => None)
  )

  def deleteAccount(using me: Me) =
    authenticator.loginCandidate.map: candidate =>
      Form:
        mapping(
          "username" -> myUsernameField,
          "passwd" -> passwordMapping(candidate),
          "understand" -> boolean.verifying("It's an important point.", identity[Boolean])
        )((_, _, _) => ())(_ => None)

  private def passwordMapping(candidate: LoginCandidate) =
    text.verifying("incorrectPassword", p => candidate.check(ClearPassword(p)))

object SecurityForm:

  case class AgreementData(
      assistance: Boolean,
      nice: Boolean,
      account: Boolean,
      policy: Boolean
  )

  trait AnySignupData:
    def username: UserName
    def email: EmailAddress
    def fp: Option[String]

  case class SignupData(
      username: UserName,
      password: String,
      email: EmailAddress,
      agreement: AgreementData,
      fp: Option[String]
  ) extends AnySignupData:
    def fingerPrint = FingerPrint.from(fp.filter(_.nonEmpty))
    def clearPassword = ClearPassword(password)

  case class MobileSignupData(
      username: UserName,
      password: String,
      email: EmailAddress
  ) extends AnySignupData:
    def fp = none

  case class PasswordReset(email: EmailAddress)

  case class MagicLink(email: EmailAddress)

  case class Reopen(username: UserStr, email: EmailAddress)

  case class ChangeEmail(passwd: String, email: EmailAddress)

  case class TwoFactor(secret: String, passwd: String, token: String):
    def tokenValid = TotpSecret.decode(secret).verify(TotpToken(token))
