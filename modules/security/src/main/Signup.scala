package lila.security

import play.api.data.*
import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

import lila.common.HTTPRequest
import lila.core.config.NetConfig
import lila.core.email.UserIdOrEmail
import lila.core.net.{ ApiVersion, IpAddress }
import lila.core.security.ClearPassword
import lila.memo.RateLimit

final class Signup(
    store: SessionStore,
    api: SecurityApi,
    ipTrust: IpTrust,
    forms: SecurityForm,
    emailConfirm: EmailConfirm,
    hcaptcha: Hcaptcha,
    passwordHasher: PasswordHasher,
    authenticator: Authenticator,
    userRepo: lila.user.UserRepo,
    disposableEmailAttempt: DisposableEmailAttempt,
    netConfig: NetConfig
)(using Executor, lila.core.config.RateLimit):

  private enum MustConfirmEmail(val value: Boolean):
    case Nope                   extends MustConfirmEmail(false)
    case YesBecausePrintExists  extends MustConfirmEmail(true)
    case YesBecausePrintMissing extends MustConfirmEmail(true)
    case YesBecauseIpExists     extends MustConfirmEmail(true)
    case YesBecauseIpSusp       extends MustConfirmEmail(true)
    case YesBecauseMobile       extends MustConfirmEmail(true)
    case YesBecauseUA           extends MustConfirmEmail(true)
    case YesBecauseEmailDomain  extends MustConfirmEmail(true)

  private object MustConfirmEmail:
    def apply(print: Option[FingerPrint], email: EmailAddress, suspIp: Boolean)(using
        req: RequestHeader
    ): Fu[MustConfirmEmail] =
      val ip = HTTPRequest.ipAddress(req)
      store.recentByIpExists(ip, 7.days).flatMap { ipExists =>
        if ipExists then fuccess(YesBecauseIpExists)
        else if UserAgentParser.trust.isSuspicious(req) then fuccess(YesBecauseUA)
        else
          print.fold[Fu[MustConfirmEmail]](fuccess(YesBecausePrintMissing)): fp =>
            store
              .recentByPrintExists(fp)
              .map: printFound =>
                if printFound then YesBecausePrintExists
                else if suspIp then YesBecauseIpSusp
                else if email.domain.exists: dom =>
                    DisposableEmailDomain.whitelisted(dom) && !DisposableEmailDomain.isOutlook(dom)
                then Nope
                else YesBecauseEmailDomain
      }

  def website(
      blind: Boolean
  )(using req: Request[?], lang: Lang, formBinding: FormBinding): Fu[Signup.Result] =
    val ip = HTTPRequest.ipAddress(req)
    forms.signup.website.flatMap:
      _.form
        .bindFromRequest()
        .fold[Fu[Signup.Result]](
          err =>
            fuccess:
              disposableEmailAttempt.onFail(err, HTTPRequest.ipAddress(req))
              Signup.Result.Bad(err.tap(signupErrLog))
          ,
          data =>
            for
              suspIp  <- ipTrust.isSuspicious(ip)
              ipData  <- ipTrust.data(ip)
              captcha <- hcaptcha.verify()
              result <- captcha match
                case Hcaptcha.Result.Fail => fuccess(Signup.Result.MissingCaptcha)
                case _ =>
                  signupRateLimit(
                    data.username.id,
                    suspIp = suspIp,
                    captched = captcha == Hcaptcha.Result.Valid
                  ):
                    MustConfirmEmail(data.fingerPrint, data.email, suspIp = suspIp).flatMap { mustConfirm =>
                      monitor(data, captcha, mustConfirm, ipData, ipSusp = suspIp, api = none)
                      lila.mon.user.register.mustConfirmEmail(mustConfirm.toString).increment()
                      val passwordHash = authenticator.passEnc(data.clearPassword)
                      userRepo
                        .create(
                          data.username,
                          passwordHash,
                          data.email,
                          blind,
                          none,
                          mustConfirmEmail = mustConfirm.value
                        )
                        .orFail(s"No user could be created for ${data.username}")
                        .addEffect:
                          logSignup(req, _, data.email, data.fingerPrint, none, captcha, mustConfirm)
                        .flatMap:
                          confirmOrAllSet(data.email, mustConfirm, data.fingerPrint, none)
                    }
            yield result
        )

  private def confirmOrAllSet(
      email: EmailAddress,
      mustConfirm: MustConfirmEmail,
      fingerPrint: Option[FingerPrint],
      apiVersion: Option[ApiVersion]
  )(user: User)(using RequestHeader, Lang): Fu[Signup.Result] =
    store.deletePreviousSessions(user) >> {
      if mustConfirm.value then
        emailConfirm.send(user, email) >> {
          if emailConfirm.effective then
            api.saveSignup(user.id, apiVersion, fingerPrint).inject(Signup.Result.ConfirmEmail(user, email))
          else fuccess(Signup.Result.AllSet(user, email))
        }
      else fuccess(Signup.Result.AllSet(user, email))
    }

  def mobile(
      apiVersion: ApiVersion
  )(using req: Request[?])(using Lang, FormBinding): Fu[Signup.Result] =
    val ip = HTTPRequest.ipAddress(req)
    forms.signup.mobile
      .bindFromRequest()
      .fold[Fu[Signup.Result]](
        err =>
          fuccess:
            disposableEmailAttempt.onFail(err, HTTPRequest.ipAddress(req))
            Signup.Result.Bad(err.tap(signupErrLog))
        ,
        data =>
          for
            suspIp <- ipTrust.isSuspicious(ip)
            ipData <- ipTrust.data(ip)
            result <- signupRateLimit(data.username.id, suspIp = suspIp, captched = false):
              val mustConfirm = MustConfirmEmail.YesBecauseMobile
              monitor(
                data,
                captcha = Hcaptcha.Result.Mobile,
                mustConfirm,
                ipData,
                suspIp,
                apiVersion.some
              )
              lila.mon.user.register.mustConfirmEmail(mustConfirm.toString).increment()
              val passwordHash = authenticator.passEnc(ClearPassword(data.password))
              userRepo
                .create(
                  data.username,
                  passwordHash,
                  data.email,
                  blind = false,
                  apiVersion.some,
                  mustConfirmEmail = mustConfirm.value
                )
                .orFail(s"No user could be created for ${data.username}")
                .addEffect:
                  logSignup(req, _, data.email, none, apiVersion.some, Hcaptcha.Result.Mobile, mustConfirm)
                .flatMap:
                  confirmOrAllSet(data.email, mustConfirm, none, apiVersion.some)
          yield result
      )

  private def monitor(
      data: SecurityForm.AnySignupData,
      captcha: Hcaptcha.Result,
      confirm: MustConfirmEmail,
      ipData: IpTrust.IpData,
      ipSusp: Boolean,
      api: Option[ApiVersion]
  ) =
    lila.mon.user.register
      .count(
        data.email.domain,
        confirm = confirm.toString,
        captcha = captcha.toString,
        ipSusp = ipSusp,
        fp = data.fp.isDefined,
        proxy = ipData.proxy.name,
        country = ipData.location.shortCountry,
        dispAttempts = disposableEmailAttempt.count(data.username.id),
        api
      )
      .increment()

  private lazy val signupRateLimitPerIP = RateLimit.composite[IpAddress](
    key = "account.create.ip"
  )(
    ("fast", 10, 10.minutes),
    ("slow", 150, 1.day)
  )

  private val rateLimitDefault = fuccess(Signup.Result.RateLimited)

  private def signupRateLimit(id: UserId, suspIp: Boolean, captched: Boolean)(
      f: => Fu[Signup.Result]
  )(using req: RequestHeader): Fu[Signup.Result] =
    val ipCost = (if suspIp then 2 else 1) * (if captched then 1 else 2)
    passwordHasher
      .rateLimit[Signup.Result](
        rateLimitDefault,
        enforce = netConfig.rateLimit,
        userCost = 1,
        ipCost = ipCost
      )(id.into(UserIdOrEmail), req): _ =>
        signupRateLimitPerIP(HTTPRequest.ipAddress(req), rateLimitDefault, cost = ipCost)(f)

  private def logSignup(
      req: RequestHeader,
      user: User,
      email: EmailAddress,
      fingerPrint: Option[FingerPrint],
      apiVersion: Option[ApiVersion],
      captcha: Hcaptcha.Result,
      mustConfirm: MustConfirmEmail
  ) =
    disposableEmailAttempt.onSuccess(user, email, HTTPRequest.ipAddress(req))
    authLog(
      user.username.into(UserStr),
      email.value,
      s"fp: $fingerPrint mustConfirm: $mustConfirm captcha: $captcha fp: ${fingerPrint
          .so(_.value)} ip: ${HTTPRequest.ipAddress(req)} api: $apiVersion"
    )

  private def signupErrLog(err: Form[?]) = for
    username <- err("username").value
    email    <- err("email").value
  yield
    if err.errors.exists(_.messages.contains("error.email_acceptable")) &&
      err("email").value.exists(EmailAddress.isValid)
    then authLog(UserStr(username), email, "Signup with unacceptable email")

  private def authLog(user: UserStr, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

object Signup:

  enum Result:
    case Bad(err: Form[?])
    case MissingCaptcha
    case RateLimited
    case ConfirmEmail(user: User, email: EmailAddress)
    case AllSet(user: User, email: EmailAddress)
