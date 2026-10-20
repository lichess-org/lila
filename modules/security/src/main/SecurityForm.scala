package lila.security

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints
import play.api.mvc.RequestHeader

import lila.common.Form.*
import lila.common.{ Form as LilaForm, LameName }

final class SecurityForm(
    userRepo: lila.user.UserRepo,
    emailValidator: EmailAddressValidator,
    lameNameCheck: LameNameCheck,
    hcaptcha: Hcaptcha
)(using ec: Executor, mode: play.api.Mode):

  import SecurityForm.*

  private val newPasswordField =
    nonEmptyText(minLength = 4, maxLength = 999).verifying(PasswordCheck.newConstraint)

  private val anyEmail: Mapping[EmailAddress] =
    LilaForm
      .cleanNonEmptyText(minLength = 6, maxLength = EmailAddress.maxLength)
      .verifying(Constraints.emailAddress)
      .verifying("error.email", EmailAddress.isValid)
      .into[EmailAddress]

  private val sendableEmail = anyEmail.verifying(emailValidator.sendableConstraint)

  private def fullyValidEmail(using me: Option[Me]) = sendableEmail
    .verifying(emailValidator.withAcceptableDns)
    .verifying(emailValidator.uniqueConstraint(me))

  private val preloadEmailDnsForm = Form(single("email" -> sendableEmail))

  def preloadEmailDns()(using req: play.api.mvc.Request[?], formBinding: FormBinding): Funit =
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
        u => !userRepo.usernameExists(u).await(3.seconds, "signupUsername")
      )

    private val agreementBool = boolean.verifying(b => b)

    private val agreement = mapping(
      "assistance" -> agreementBool,
      "nice"       -> agreementBool,
      "account"    -> agreementBool,
      "policy"     -> agreementBool
    )(AgreementData.apply)(unapply)

    def website(using RequestHeader) = hcaptcha.form:
      Form:
        mapping(
          "username"  -> username,
          "password"  -> newPasswordField,
          "email"     -> emailField,
          "agreement" -> agreement,
          "fp"        -> optional(nonEmptyText)
        )(SignupData.apply)(_ => None)
          .verifying(PasswordCheck.errorSame, x => mode.notProd || x.password != x.username.value)

    val mobile = Form:
      mapping(
        "username" -> username,
        "password" -> newPasswordField,
        "email"    -> emailField
      )(MobileSignupData.apply)(_ => None)
        .verifying(PasswordCheck.errorSame, x => mode.notProd || x.password != x.username.value)

  def modEmail(user: User) = Form(
    single("email" -> optional(anyEmail.verifying(emailValidator.uniqueConstraint(user.some))))
  )

  /** Confirmation by display name only: SSO accounts have no usable local password. */
  private def displayUsernameField(using me: Me) =
    text.verifying("Please enter your username to confirm.", _.trim == me.username.value)

  def closeAccount(using Me) =
    Form:
      mapping(
        "username" -> displayUsernameField,
        "forever"  -> boolean
      )((_, forever) => forever)(_ => None)

  def toggleKid(using Me) =
    Form(single("confirm" -> boolean.verifying("Please confirm this change.", identity[Boolean])))

  def reopenConfirm(using Me) =
    Form(
      single(
        "confirm" -> boolean.verifying("Please confirm you want to reopen your account.", identity[Boolean])
      )
    )

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
    def fingerPrint   = FingerPrint.from(fp.filter(_.nonEmpty))
    def clearPassword = lila.core.security.ClearPassword(password)

  case class MobileSignupData(
      username: UserName,
      password: String,
      email: EmailAddress
  ) extends AnySignupData:
    def fp = none
