package lila.security

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.common.{ Bus, Strings }
import lila.memo.SettingStore.Strings._
import lila.oauth.OAuthServer
import lila.user.{ Authenticator, UserRepo }

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    net: NetConfig,
    captcher: lila.hub.actors.Captcher,
    userRepo: UserRepo,
    authenticator: Authenticator,
    slack: lila.slack.SlackApi,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    tryOAuthServer: OAuthServer.Try,
    mongoCache: lila.memo.MongoCache.Api,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    scheduler: Scheduler
) {
  import net.{ baseUrl, domain }

  private val config = appConfig.get[SecurityConfig]("security")(SecurityConfig.loader)

  private def recaptchaPublicConfig = config.recaptcha.public

  def recaptcha[A](formId: String, form: play.api.data.Form[A]) =
    RecaptchaForm(form, formId, config.recaptcha.public)

  lazy val firewall = new Firewall(
    coll = db(config.collection.firewall),
    scheduler = scheduler
  )

  lazy val flood = wire[Flood]

  lazy val recaptcha: Recaptcha =
    if (config.recaptcha.enabled) wire[RecaptchaGoogle]
    else RecaptchaSkip

  lazy val forms = wire[SecurityForm]

  lazy val geoIP: GeoIP = wire[GeoIP]

  lazy val userSpy = wire[UserSpyApi]

  lazy val store = new Store(db(config.collection.security), cacheApi, net.ip)

  lazy val ip2proxy: Ip2Proxy =
    if (config.ip2Proxy.enabled) {
      def mk = (url: String) => wire[Ip2ProxyServer]
      mk(config.ip2Proxy.url)
    } else wire[Ip2ProxySkip]

  lazy val ugcArmedSetting = settingStore[Boolean](
    "ugcArmed",
    default = true,
    text = "Enable the user garbage collector".some
  )

  lazy val printBan = new PrintBan(db(config.collection.printBan))

  lazy val garbageCollector = {
    def mk: (() => Boolean) => GarbageCollector = isArmed => wire[GarbageCollector]
    mk(ugcArmedSetting.get _)
  }

  private lazy val mailgun: Mailgun = wire[Mailgun]

  lazy val emailConfirm: EmailConfirm =
    if (config.emailConfirm.enabled)
      new EmailConfirmMailgun(
        userRepo = userRepo,
        mailgun = mailgun,
        baseUrl = baseUrl,
        tokenerSecret = config.emailConfirm.secret
      )
    else wire[EmailConfirmSkip]

  lazy val passwordReset = {
    def mk = (s: Secret) => wire[PasswordReset]
    mk(config.passwordResetSecret)
  }

  lazy val magicLink = {
    def mk = (s: Secret) => wire[MagicLink]
    mk(config.passwordResetSecret)
  }

  lazy val reopen = {
    def mk = (s: Secret) => wire[Reopen]
    mk(config.passwordResetSecret)
  }

  lazy val emailChange = {
    def mk = (s: Secret) => wire[EmailChange]
    mk(config.emailChangeSecret)
  }

  lazy val loginToken = new LoginToken(config.loginTokenSecret, userRepo)

  lazy val automaticEmail = wire[AutomaticEmail]

  lazy val signup = wire[Signup]

  private lazy val dnsApi: DnsApi = wire[DnsApi]

  private lazy val checkMail: CheckMail = wire[CheckMail]

  lazy val emailAddressValidator = wire[EmailAddressValidator]

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    ws = ws,
    providerUrl = config.disposableEmail.providerUrl,
    checkMailBlocked = () => checkMail.fetchAllBlocked
  )

  // import reactivemongo.api.bson._

  lazy val spamKeywordsSetting = settingStore[Strings](
    "spamKeywords",
    default = Strings(Nil),
    text = "Spam keywords separated by a comma".some
  )

  lazy val spam = new Spam(spamKeywordsSetting.get _)

  lazy val promotion = wire[PromotionApi]

  if (config.disposableEmail.enabled) {
    scheduler.scheduleOnce(30 seconds)(disposableEmailDomain.refresh())
    scheduler.scheduleWithFixedDelay(
      config.disposableEmail.refreshDelay,
      config.disposableEmail.refreshDelay
    ) { () =>
      disposableEmailDomain.refresh()
    }
  }

  lazy val tor: Tor = wire[Tor]

  if (config.tor.enabled) {
    scheduler.scheduleOnce(31 seconds)(tor.refresh.unit)
    scheduler.scheduleWithFixedDelay(config.tor.refreshDelay, config.tor.refreshDelay) { () =>
      tor.refresh flatMap firewall.unblockIps
      ()
    }
  }

  lazy val ipTrust: IpTrust = wire[IpTrust]

  lazy val api = wire[SecurityApi]

  lazy val csrfRequestHandler = wire[CSRFRequestHandler]

  def cli = wire[Cli]

  lila.common.Bus.subscribeFuns(
    "fishnet" -> { case lila.hub.actorApi.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key).unit
    },
    "gdprErase" -> { case lila.user.User.GDPRErase(user) =>
      store.definitelyEraseAllUserInfo(user).unit
    }
  )
}
