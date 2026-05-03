package lila.security

import com.softwaremill.tagging.*
import play.api.data.*
import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }
import scalalib.cache.ExpireSetMemo

import lila.common.HTTPRequest
import lila.core.config.NetConfig
import lila.core.email.UserIdOrEmail
import lila.core.net.{ ApiVersion, IpAddress, ValidReferrer }
import lila.memo.{ RateLimit, SettingStore }
import lila.security.SecurityForm.SignupData
import lila.oauth.Protocol.ClientId

final class Signup(
    store: SessionStore,
    api: SecurityApi,
    ipTrust: IpTrust,
    canSendEmails: SettingStore[Boolean] @@ lila.mailer.CanSendEmails,
    forms: SecurityForm,
    emailConfirm: EmailConfirm,
    turnstile: Turnstile,
    passwordHasher: PasswordHasher,
    authenticator: Authenticator,
    userRepo: lila.user.UserRepo,
    disposableEmailAttempt: DisposableEmailAttempt,
    pwnedApi: PwnedApi,
    cacheApi: lila.memo.CacheApi,
    netConfig: NetConfig
)(using Executor, lila.core.config.RateLimit):

  private enum MustConfirmEmail(val value: Boolean):
    case NoCantSend extends MustConfirmEmail(false)
    case NoSimpleSignup extends MustConfirmEmail(false)
    case YesAnyway extends MustConfirmEmail(true)
    case YesBecausePrintExists extends MustConfirmEmail(true)
    case YesBecausePrintMissing extends MustConfirmEmail(true)
    case YesBecauseIpExists extends MustConfirmEmail(true)
    case YesBecauseIpSusp extends MustConfirmEmail(true)
    case YesBecauseUA extends MustConfirmEmail(true)
    case YesBecauseEmailDomain extends MustConfirmEmail(true)

  private object MustConfirmEmail:
    def apply(
        data: SignupData,
        suspIp: Boolean,
        simpleSignup: Option[lila.oauth.OAuthSignedClient.SimpleSignup]
    )(using req: RequestHeader): Fu[MustConfirmEmail] =
      if !canSendEmails.get() then fuccess(NoCantSend)
      else if simpleSignup.exists(_.email == data.email) then fuccess(NoSimpleSignup)
      else
        val ip = HTTPRequest.ipAddress(req)
        store.recentByIpExists(ip, 7.days).flatMap { ipExists =>
          if ipExists then fuccess(YesBecauseIpExists)
          else if UserAgentParser.trust.isSuspicious(req) then fuccess(YesBecauseUA)
          else
            data.fingerPrint.fold[Fu[MustConfirmEmail]](fuccess(YesBecausePrintMissing)): fp =>
              store
                .recentByPrintExists(fp)
                .map: printFound =>
                  if printFound then YesBecausePrintExists
                  else if suspIp then YesBecauseIpSusp
                  else if data.email.domain.exists: dom =>
                      DisposableEmailDomain.whitelisted(dom) && !DisposableEmailDomain.isOutlook(dom)
                  then YesAnyway
                  else YesBecauseEmailDomain
        }

  private val dedupCache = cacheApi.notLoading[SecurityForm.AnySignupData, Signup.Result](16, "signup.dedup"):
    _.expireAfterWrite(3.seconds).buildAsync()

  private val dedupSimpleSignupEmail = ExpireSetMemo[EmailAddress](1.day)

  def website(
      blind: Boolean,
      simpleSignup: Option[lila.oauth.OAuthSignedClient.SimpleSignup]
  )(using
      req: Request[?],
      lang: Lang,
      formBinding: FormBinding,
      referrer: Option[ValidReferrer]
  ): Fu[Signup.Result] =
    val client = simpleSignup.fold("website")(_.client.value)
    val turnstileSuccess = if simpleSignup.isDefined then fuccess(true)
    else turnstile.verify()
    turnstileSuccess
      .flatMap: turnstileSuccess =>
        if !turnstileSuccess then fuccess(Signup.Result.TurnstileFail)
        else
          forms.preloadEmailDns() >>
            forms.signup
              .website(simpleSignup)
              .form
              .bindFromRequest()
              .fold[Fu[Signup.Result]](
                err =>
                  fuccess:
                    disposableEmailAttempt.onFail(err, HTTPRequest.ipAddress(req))
                    Signup.Result.FormInvalid(err.tap(signupErrLog))
                ,
                data =>
                  dedupCache.getFuture(
                    data,
                    _ =>
                      if simpleSignup.exists(s => dedupSimpleSignupEmail.get(s.email))
                      then fuccess(Signup.Result.SimpleSignupDuplicate)
                      else
                        for
                          suspIp <- ipTrust.isSuspicious(HTTPRequest.ipAddress(req))
                          ipData <- ipTrust.reqData(req)
                          pwned <- pwnedApi.isPwned(data.clearPassword)
                          result <- signupRateLimit(data.username.id, suspIp = suspIp):
                            MustConfirmEmail(data, suspIp = suspIp, simpleSignup).flatMap: mustConfirm =>
                              val passwordHash = authenticator.passEnc(data.clearPassword)
                              userRepo
                                .create(
                                  data.username,
                                  passwordHash,
                                  data.email,
                                  blind = blind,
                                  mustConfirmEmail = mustConfirm.value
                                )
                                .orFail(s"No user could be created for ${data.username}")
                                .addEffect: user =>
                                  monitor(
                                    data,
                                    mustConfirm,
                                    ipData,
                                    ipSusp = suspIp,
                                    client = client
                                  )
                                  logSignup(
                                    req,
                                    user,
                                    data.email,
                                    data.fingerPrint,
                                    mustConfirm,
                                    pwned
                                  )
                                  simpleSignup.foreach(s => dedupSimpleSignupEmail.put(s.email))
                                .flatMap:
                                  confirmOrAllSet(data.email, mustConfirm, data.fingerPrint, none, pwned)
                        yield result
                  )
              )
      .addEffect: res =>
        lila.mon.user.register.result(client, res.key).increment()

  private def confirmOrAllSet(
      email: EmailAddress,
      mustConfirm: MustConfirmEmail,
      fingerPrint: Option[FingerPrint],
      apiVersion: Option[ApiVersion],
      pwned: IsPwned
  )(user: User)(using RequestHeader, Lang, Option[ValidReferrer]): Fu[Signup.Result] =
    store.deletePreviousSessions(user) >> {
      if mustConfirm.value then
        emailConfirm.send(user, email) >> {
          if emailConfirm.effective then
            for _ <- api.saveSignup(user.id, apiVersion, fingerPrint, pwned)
            yield Signup.Result.ConfirmEmail(user, email)
          else fuccess(Signup.Result.AllSet(user, email))
        }
      else fuccess(Signup.Result.AllSet(user, email))
    }

  private def monitor(
      data: SecurityForm.AnySignupData,
      confirm: MustConfirmEmail,
      ipData: IpTrust.IpData,
      ipSusp: Boolean,
      client: String
  ) =
    lila.mon.user.register
      .count(
        confirm = confirm.toString,
        ipSusp = ipSusp,
        fp = data.fp.isDefined,
        proxy = ipData.proxy.name,
        country = ipData.location.shortCountry,
        client = client
      )
      .increment()
    lila.mon.user.register.mustConfirmEmail(confirm.toString).increment()

  private lazy val signupRateLimitPerIP = RateLimit.composite[IpAddress](
    key = "account.create.ip"
  )(
    ("fast", 10, 10.minutes),
    ("slow", 150, 1.day)
  )

  private val rateLimitDefault = fuccess(Signup.Result.RateLimited)

  private def signupRateLimit(id: UserId, suspIp: Boolean)(
      f: => Fu[Signup.Result]
  )(using req: RequestHeader): Fu[Signup.Result] =
    val ipCost = if suspIp then 2 else 1
    passwordHasher
      .rateLimit[Signup.Result](
        rateLimitDefault,
        enforce = netConfig.rateLimit,
        ipCost = ipCost
      )(id.into(UserIdOrEmail), req): _ =>
        signupRateLimitPerIP(HTTPRequest.ipAddress(req), rateLimitDefault, cost = ipCost)(f)

  private def logSignup(
      req: RequestHeader,
      user: User,
      email: EmailAddress,
      fingerPrint: Option[FingerPrint],
      mustConfirm: MustConfirmEmail,
      pwned: IsPwned
  ) =
    disposableEmailAttempt.onSuccess(user, email, HTTPRequest.ipAddress(req))
    authLog(
      user.username.into(UserStr),
      email.value,
      s"fp: $fingerPrint mustConfirm: $mustConfirm fp: ${fingerPrint.so(_.value)} ip: ${HTTPRequest.ipAddress(req)} pwned: $pwned"
    )

  private def signupErrLog(err: Form[?]) = for
    username <- err("username").value
    email <- err("email").value
  yield
    if err.errors.exists(_.messages.contains("error.email_acceptable")) &&
      err("email").value.exists(EmailAddress.isValid)
    then authLog(UserStr(username), email, "Signup with unacceptable email")

  private def authLog(user: UserStr, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

object Signup:

  enum Result(val key: String):
    case FormInvalid(err: Form[?]) extends Result("formError")
    case TurnstileFail extends Result("turnstileFail")
    case RateLimited extends Result("rateLimited")
    case SimpleSignupDuplicate extends Result("simpleSignupDuplicate")
    case ForbiddenNetwork extends Result("forbiddenNetwork")
    case ConfirmEmail(user: User, email: EmailAddress) extends Result("confirmEmail")
    case AllSet(user: User, email: EmailAddress) extends Result("allSet")
