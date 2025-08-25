package lila.security

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.*
import lila.core.data.Strings
import lila.memo.SettingStore
import lila.memo.SettingStore.Strings.given
import lila.oauth.OAuthServer
import lila.common.Bus

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    net: lila.core.config.NetConfig,
    userRepo: lila.user.UserRepo,
    mailer: lila.mailer.Mailer,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    oAuthServer: OAuthServer,
    mongoCache: lila.memo.MongoCache.Api,
    canSendEmails: SettingStore[Boolean] @@ lila.mailer.CanSendEmails,
    cookieBaker: play.api.mvc.SessionCookieBaker,
    lazyCurrentlyPlaying: => lila.core.round.CurrentlyPlaying,
    db: lila.db.Db
)(using Executor, play.api.Mode, lila.core.i18n.Translator, lila.core.config.RateLimit)(using
    scheduler: Scheduler
):

  private val (baseUrl, domain) = (net.baseUrl, net.domain)

  private val config = appConfig.get[SecurityConfig]("security")

  private def hcaptchaPublicConfig = config.hcaptcha.public

  val lilaCookie = wire[LilaCookie]

  lazy val firewall = Firewall(
    coll = db(config.collection.firewall),
    config = config,
    scheduler = scheduler,
    ws = ws
  )

  lazy val flood = new Flood

  lazy val passwordHasher = PasswordHasher(
    secret = config.passwordBPassSecret,
    logRounds = 10,
    hashTimer = lila.common.Chronometer.syncMon(_.user.auth.hashTime)
  )

  lazy val authenticator = wire[Authenticator]

  lazy val hcaptcha: Hcaptcha =
    if config.hcaptcha.enabled then wire[HcaptchaReal]
    else wire[HcaptchaSkip]

  lazy val forms = wire[SecurityForm]
  def signupForm: lila.core.security.SignupForm = forms.signup

  lazy val geoIP: GeoIP = wire[GeoIP]

  lazy val userLogins = wire[UserLoginsApi]

  lazy val store = SessionStore(db(config.collection.security), cacheApi)

  private lazy val tor: Tor = wire[Tor]

  lazy val ip2proxy: lila.core.security.Ip2ProxyApi =
    if config.ip2Proxy.enabled && config.ip2Proxy.url.nonEmpty then
      def mk = (url: String) => wire[Ip2ProxyServer]
      mk(config.ip2Proxy.url)
    else wire[Ip2ProxySkip]

  lazy val ugcArmedSetting = settingStore[Boolean](
    "ugcArmed",
    default = true,
    text = "Enable the user garbage collector".some
  )

  lazy val printBan = PrintBan(db(config.collection.printBan))

  private val curPlaying = lila.core.data.LazyDep(() => lazyCurrentlyPlaying)

  lazy val garbageCollector =
    def mk: (() => Boolean) => GarbageCollector = isArmed => wire[GarbageCollector]
    mk(() => ugcArmedSetting.get())

  lazy val emailConfirm: EmailConfirm =
    if config.emailConfirm.enabled then
      EmailConfirmMailer(
        userRepo = userRepo,
        mailer = mailer,
        baseUrl = baseUrl,
        tokenerSecret = config.emailConfirm.secret
      )
    else wire[EmailConfirmSkip]

  lazy val passwordReset =
    def mk = (s: Secret) => wire[PasswordReset]
    mk(config.passwordResetSecret)

  lazy val reopen =
    def mk = (s: Secret) => wire[Reopen]
    mk(config.passwordResetSecret)

  lazy val emailChange =
    def mk = (s: Secret) => wire[EmailChange]
    mk(config.emailChangeSecret)

  lazy val loginToken =
    def mk = (s: Secret) => wire[LoginToken]
    mk(config.loginTokenSecret)

  lazy val disposableEmailAttempt = wire[DisposableEmailAttempt]

  lazy val signup = wire[Signup]

  private lazy val dnsApi: DnsApi = wire[DnsApi]

  private lazy val verifyMail: VerifyMail = wire[VerifyMail]

  lazy val emailAddressValidator = wire[EmailAddressValidator]

  private lazy val disposableEmailDomain = DisposableEmailDomain(
    ws = ws,
    providerUrl = config.disposableEmail.providerUrl,
    verifyMailBlocked = () => verifyMail.fetchAllBlocked
  )

  lazy val spamKeywordsSetting = settingStore[Strings](
    "spamKeywords",
    default = Strings(Nil),
    text = "Spam keywords separated by a comma".some
  )

  lazy val spam = Spam(spamKeywordsSetting.get)

  lazy val promotion = wire[PromotionApi]

  if config.disposableEmail.enabled then
    scheduler.scheduleWithFixedDelay(42.seconds, 1.hour): () =>
      disposableEmailDomain.refresh()

  lazy val ipTrust: IpTrust = wire[IpTrust]

  lazy val userTrust: UserTrustApi = wire[UserTrustApi]

  lazy val pwned: PwnedApi = PwnedApi(ws, config.pwnedRangeUrl)

  lazy val proxy2faSetting: SettingStore[Strings] @@ Proxy2faSetting = settingStore[Strings](
    "proxy2fa",
    default = Strings(List("PUB", "TOR")),
    text = "Types of proxy that require 2FA to login".some
  ).taggedWith[Proxy2faSetting]

  val mobileSignupProxy = settingStore[Strings](
    "mobileSignupProxy",
    default = Strings(List("VPN", "CPN")),
    text = "Types of proxy that can signup using the legacy mobile API".some
  ).taggedWith[MobileSignupProxy]

  val alwaysCaptcha = settingStore[Boolean](
    "alwaysCaptcha",
    default = false,
    text = "Always serve captchas, don't skip once per IP and per 24h".some
  ).taggedWith[AlwaysCaptcha]

  lazy val api = wire[SecurityApi]

  lazy val csrfRequestHandler = wire[CSRFRequestHandler]

  lazy val cli = wire[Cli]

  lazy val coreApi = new lila.core.security.SecurityApi:
    export api.shareAnIpOrFp
    export userLogins.getUserIdsWithSameIpAndPrint

  Bus.sub[lila.core.security.AskAreRelated]: ask =>
    ask.promise.completeWith(api.shareAnIpOrFp.tupled(ask.users))

private trait Proxy2faSetting
private trait MobileSignupProxy
private trait AlwaysCaptcha
