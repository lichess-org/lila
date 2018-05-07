package lila.security

import lila.oauth.OAuthServer

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    captcher: ActorSelection,
    authenticator: lila.user.Authenticator,
    slack: lila.slack.SlackApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder,
    tryOAuthServer: OAuthServer.Try,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val MailgunApiUrl = config getString "mailgun.api.url"
    val MailgunApiKey = config getString "mailgun.api.key"
    val MailgunSender = config getString "mailgun.sender"
    val MailgunReplyTo = config getString "mailgun.reply_to"
    val CollectionSecurity = config getString "collection.security"
    val FirewallEnabled = config getBoolean "firewall.enabled"
    val FirewallCookieName = config getString "firewall.cookie.name"
    val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
    val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
    val FloodDuration = config duration "flood.duration"
    val GeoIPFile = config getString "geoip.file"
    val GeoIPCacheTtl = config duration "geoip.cache_ttl"
    val EmailConfirmSecret = config getString "email_confirm.secret"
    val EmailConfirmEnabled = config getBoolean "email_confirm.enabled"
    val PasswordResetSecret = config getString "password_reset.secret"
    val EmailChangeSecret = config getString "email_change.secret"
    val LoginTokenSecret = config getString "login_token.secret"
    val TorProviderUrl = config getString "tor.provider_url"
    val TorRefreshDelay = config duration "tor.refresh_delay"
    val DisposableEmailProviderUrl = config getString "disposable_email.provider_url"
    val DisposableEmailRefreshDelay = config duration "disposable_email.refresh_delay"
    val RecaptchaPrivateKey = config getString "recaptcha.private_key"
    val RecaptchaEndpoint = config getString "recaptcha.endpoint"
    val NetBaseUrl = config getString "net.base_url"
    val NetDomain = config getString "net.domain"
    val NetEmail = config getString "net.email"
  }
  import settings._

  val recaptchaPublicConfig = RecaptchaPublicConfig(
    key = config getString "recaptcha.public_key",
    enabled = config getBoolean "recaptcha.enabled"
  )

  lazy val firewall = new Firewall(
    coll = firewallColl,
    cookieName = FirewallCookieName.some filter (_ => FirewallCookieEnabled),
    enabled = FirewallEnabled,
    system = system
  )

  lazy val flood = new Flood(FloodDuration)

  lazy val recaptcha: Recaptcha =
    if (recaptchaPublicConfig.enabled) new RecaptchaGoogle(
      privateKey = RecaptchaPrivateKey,
      endpoint = RecaptchaEndpoint,
      lichessHostname = NetDomain
    )
    else RecaptchaSkip

  lazy val forms = new DataForm(
    captcher = captcher,
    authenticator = authenticator,
    emailValidator = emailAddressValidator
  )

  lazy val geoIP = new GeoIP(
    file = GeoIPFile,
    cacheTtl = GeoIPCacheTtl
  )

  lazy val userSpyApi = new UserSpyApi(firewall, geoIP, storeColl)
  def userSpy = userSpyApi.apply _

  def store = Store

  lazy val ipIntel = new IpIntel(asyncCache, NetEmail)

  lazy val ugcArmedSetting = settingStore[Boolean](
    "ugcArmed",
    default = true,
    text = "Enable the user garbage collector".some
  )

  lazy val garbageCollector = new GarbageCollector(
    userSpyApi,
    ipTrust,
    slack,
    ugcArmedSetting.get,
    system
  )

  private lazy val mailgun = new Mailgun(
    apiUrl = MailgunApiUrl,
    apiKey = MailgunApiKey,
    from = MailgunSender,
    replyTo = MailgunReplyTo,
    system = system
  )

  lazy val emailConfirm: EmailConfirm =
    if (EmailConfirmEnabled) new EmailConfirmMailgun(
      mailgun = mailgun,
      baseUrl = NetBaseUrl,
      tokenerSecret = EmailConfirmSecret
    )
    else EmailConfirmSkip

  lazy val passwordReset = new PasswordReset(
    mailgun = mailgun,
    baseUrl = NetBaseUrl,
    tokenerSecret = PasswordResetSecret
  )

  lazy val emailChange = new EmailChange(
    mailgun = mailgun,
    baseUrl = NetBaseUrl,
    tokenerSecret = EmailChangeSecret
  )

  lazy val loginToken = new LoginToken(
    secret = LoginTokenSecret
  )

  lazy val welcomeEmail = new WelcomeEmail(
    mailgun = mailgun,
    baseUrl = NetBaseUrl
  )

  lazy val automaticEmail = new AutomaticEmail(
    mailgun = mailgun,
    baseUrl = NetBaseUrl
  )

  lazy val emailAddressValidator = new EmailAddressValidator(disposableEmailDomain)

  lazy val emailBlacklistSetting = settingStore[String](
    "emailBlacklist",
    default = "",
    text = "Blacklisted email domains separated by a space".some
  )

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    providerUrl = DisposableEmailProviderUrl,
    blacklistStr = emailBlacklistSetting.get,
    busOption = system.lilaBus.some
  )

  scheduler.once(15 seconds)(disposableEmailDomain.refresh)
  scheduler.effect(DisposableEmailRefreshDelay, "Refresh disposable email domains")(disposableEmailDomain.refresh)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(30 seconds)(tor.refresh(_ => funit))
  scheduler.effect(TorRefreshDelay, "Refresh Tor exit nodes")(tor.refresh(firewall.unblockIps))

  lazy val ipTrust = new IpTrust(ipIntel, geoIP, tor, firewall)

  lazy val api = new SecurityApi(storeColl, firewall, geoIP, authenticator, emailAddressValidator, tryOAuthServer)

  lazy val csrfRequestHandler = new CSRFRequestHandler(NetDomain)

  def cli = new Cli

  // api actor
  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.fishnet.NewKey(userId, key) => automaticEmail.onFishnetKey(userId, key)
    }
  })), 'fishnet)

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  private lazy val system = lila.common.PlayApp.system

  lazy val current = "security" boot new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    authenticator = lila.user.Env.current.authenticator,
    slack = lila.slack.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    settingStore = lila.memo.Env.current.settingStore,
    tryOAuthServer = () => scala.concurrent.Future {
      lila.oauth.Env.current.server.some
    }.withTimeoutDefault(50 millis, none)(system) recover {
      case e: Exception =>
        lila.log("security").warn("oauth", e)
        none
    },
    system = system,
    scheduler = lila.common.PlayApp.scheduler,
    captcher = lila.hub.Env.current.actor.captcher,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
