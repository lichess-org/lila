package lila.security

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.common.{ Bus, Strings, Iso, EmailAddress }
import lila.memo.SettingStore.Formable.stringsFormable
import lila.memo.SettingStore.Strings._
import lila.oauth.OAuthServer
import lila.user.{ UserRepo, Authenticator }

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    captcher: ActorSelection,
    userRepo: UserRepo,
    authenticator: Authenticator,
    slack: lila.slack.SlackApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder,
    tryOAuthServer: OAuthServer.Try,
    mongoCache: lila.memo.MongoCache.Builder,
    system: ActorSystem,
    scheduler: Scheduler,
    db: lila.db.Env,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val config = appConfig.get[SecurityConfig.Root]("security")(SecurityConfig.loader)

  val recaptchaPublicConfig = config.recaptcha.public

  lazy val firewall = new Firewall(
    coll = db(config.collection.firewall),
    scheduler = scheduler
  )

  lazy val flood = new Flood(config.floodDuration)

  lazy val recaptcha: Recaptcha =
    if (recaptchaPublicConfig.enabled) wire[RecaptchaGoogle]
    else RecaptchaSkip

  lazy val forms = wire[DataForm]

  lazy val geoIP = wire[GeoIP]

  lazy val userSpyApi = wire[UserSpyApi]

  lazy val store = new Store(db(config.collection.security))

  lazy val ipIntel = {
    def mk = (email: EmailAddress) => wire[IpIntel]
    mk(config.ipIntelEmail)
  }

  lazy val ugcArmedSetting = settingStore[Boolean](
    "ugcArmed",
    default = true,
    text = "Enable the user garbage collector".some
  )

  lazy val printBan = new PrintBan(db(config.collection.printBan))

  lazy val garbageCollector = {
    def mk = (isArmed: () => Boolean) => wire[GarbageCollector]
    mk(ugcArmedSetting.get)
  }

  private lazy val mailgun = wire[Mailgun]

  lazy val emailConfirm: EmailConfirm =
    if (EmailConfirmEnabled) new EmailConfirmMailgun(
      mailgun = mailgun,
      baseUrl = NetBaseUrl,
      tokenerSecret = EmailConfirmSecret
    )
    else EmailConfirmSkip

  lazy val passwordReset = {
    def mk = (u: BaseUrl, s: Secret) => wire[PasswordReset]
    mk(config.net.baseUrl, config.passwordResetSecret)
  }

  lazy val magicLink = {
    def mk = (u: BaseUrl, s: Secret) => wire[MagicLink]
    mk(config.net.baseUrl, config.passwordResetSecret)
  }

  lazy val emailChange = {
    def mk = (u: BaseUrl, s: Secret) => wire[EmailChange]
    mk(config.net.baseUrl, config.emailChangeSecret)
  }

  lazy val loginToken = new LoginToken(config.loginTokenSecret, userRepo)

  lazy val automaticEmail = wire[AutomaticEmail]

  private lazy val dnsApi = wire[DnsApi]

  private lazy val checkMail: CheckMail = wire[CheckMail]

  lazy val emailAddressValidator = wire[EmailAddressValidator]

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    ws = ws,
    providerUrl = config.disposableEmail.providerUrl,
    checkMailBlocked = () => checkMail.fetchAllBlocked
  )

  import reactivemongo.api.bson._

  lazy val spamKeywordsSetting =
    settingStore[Strings](
      "spamKeywords",
      default = Strings(Nil),
      text = "Spam keywords separated by a comma".some
    )

  lazy val spam = new Spam(spamKeywordsSetting.get)

  scheduler.once(30 seconds)(disposableEmailDomain.refresh)
  scheduler.effect(config.disposableEmail.refreshDelay, "Refresh disposable email domains")(disposableEmailDomain.refresh)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(31 seconds)(tor.refresh(_ => funit))
  scheduler.effect(TorRefreshDelay, "Refresh Tor exit nodes")(tor.refresh(firewall.unblockIps))

  lazy val ipTrust = new IpTrust(ipIntel, geoIP, tor, firewall)

  lazy val api = new SecurityApi(storeColl, firewall, geoIP, authenticator, emailAddressValidator, tryOAuthServer)(system)

  lazy val csrfRequestHandler = new CSRFRequestHandler(NetDomain)

  def cli = new Cli

  Bus.subscribeFun("fishnet") {
    case lila.hub.actorApi.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key)(lila.i18n.defaultLang)
  }
}
