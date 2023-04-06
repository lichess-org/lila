package lila.security

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.common.Strings
import lila.memo.SettingStore.Strings.given
import lila.oauth.OAuthServer
import lila.user.{ Authenticator, UserRepo }

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    net: NetConfig,
    userRepo: UserRepo,
    authenticator: Authenticator,
    mailer: lila.mailer.Mailer,
    irc: lila.irc.IrcApi,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    oAuthServer: OAuthServer,
    mongoCache: lila.memo.MongoCache.Api,
    db: lila.db.Db
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    mode: play.api.Mode
):
  private val (baseUrl, domain) = (net.baseUrl, net.domain)

  private val config = appConfig.get[SecurityConfig]("security")

  private def hcaptchaPublicConfig = config.hcaptcha.public

  lazy val firewall = Firewall(
    coll = db(config.collection.firewall),
    scheduler = scheduler
  )

  lazy val flood = wire[Flood]

  lazy val hcaptcha: Hcaptcha =
    if (config.hcaptcha.enabled) wire[HcaptchaReal]
    else wire[HcaptchaSkip]

  lazy val forms = wire[SecurityForm]

  lazy val geoIP: GeoIP = wire[GeoIP]

  lazy val userLogins = wire[UserLoginsApi]

  lazy val store = Store(db(config.collection.security), cacheApi)

  lazy val ip2proxy: Ip2Proxy =
    if (config.ip2Proxy.enabled && config.ip2Proxy.url.nonEmpty)
      def mk = (url: String) => wire[Ip2ProxyServer]
      mk(config.ip2Proxy.url)
    else wire[Ip2ProxySkip]

  lazy val ugcArmedSetting = settingStore[Boolean](
    "ugcArmed",
    default = true,
    text = "Enable the user garbage collector".some
  )

  lazy val printBan = PrintBan(db(config.collection.printBan))

  lazy val garbageCollector =
    def mk: (() => Boolean) => GarbageCollector = isArmed => wire[GarbageCollector]
    mk((() => ugcArmedSetting.get()))

  lazy val emailConfirm: EmailConfirm =
    if (config.emailConfirm.enabled)
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

  lazy val magicLink =
    def mk = (s: Secret) => wire[MagicLink]
    mk(config.passwordResetSecret)

  lazy val reopen =
    def mk = (s: Secret) => wire[Reopen]
    mk(config.passwordResetSecret)

  lazy val emailChange =
    def mk = (s: Secret) => wire[EmailChange]
    mk(config.emailChangeSecret)

  lazy val loginToken = LoginToken(config.loginTokenSecret, userRepo)

  lazy val disposableEmailAttempt = wire[DisposableEmailAttempt]

  lazy val signup = wire[Signup]

  private lazy val dnsApi: DnsApi = wire[DnsApi]

  private lazy val checkMail: CheckMail = wire[CheckMail]

  lazy val emailAddressValidator = wire[EmailAddressValidator]

  private lazy val disposableEmailDomain = DisposableEmailDomain(
    ws = ws,
    providerUrl = config.disposableEmail.providerUrl,
    checkMailBlocked = () => checkMail.fetchAllBlocked
  )

  lazy val spamKeywordsSetting = settingStore[Strings](
    "spamKeywords",
    default = Strings(Nil),
    text = "Spam keywords separated by a comma".some
  )

  lazy val spam = Spam(spamKeywordsSetting.get)

  lazy val promotion = wire[PromotionApi]

  if (config.disposableEmail.enabled)
    scheduler.scheduleOnce(33 seconds)(disposableEmailDomain.refresh())
    scheduler.scheduleWithFixedDelay(
      config.disposableEmail.refreshDelay,
      config.disposableEmail.refreshDelay
    ) { () =>
      disposableEmailDomain.refresh()
    }

  lazy val tor: Tor = wire[Tor]

  if (config.tor.enabled)
    scheduler.scheduleOnce(44 seconds)(tor.refresh.unit)
    scheduler.scheduleWithFixedDelay(config.tor.refreshDelay, config.tor.refreshDelay) { () =>
      tor.refresh flatMap firewall.unblockIps
      ()
    }

  lazy val ipTrust: IpTrust = wire[IpTrust]

  lazy val pwned: Pwned = Pwned(ws, config.pwnedUrl)

  lazy val api = wire[SecurityApi]

  lazy val csrfRequestHandler = wire[CSRFRequestHandler]

  lazy val cli = wire[Cli]
