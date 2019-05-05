package lila.security

import lila.common.{ EmailAddress, Strings, Iso }
import lila.memo.SettingStore.Formable.stringsFormable
import lila.memo.SettingStore.Strings._
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

  private val MailgunApiUrl = config getString "mailgun.api.url"
  private val MailgunApiKey = config getString "mailgun.api.key"
  private val MailgunSender = config getString "mailgun.sender"
  private val MailgunReplyTo = config getString "mailgun.reply_to"
  private val CollectionSecurity = config getString "collection.security"
  private val FirewallEnabled = config getBoolean "firewall.enabled"
  private val FirewallCookieName = config getString "firewall.cookie.name"
  private val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
  private val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
  private val FloodDuration = config duration "flood.duration"
  private val GeoIPFile = config getString "geoip.file"
  private val GeoIPCacheTtl = config duration "geoip.cache_ttl"
  private val EmailConfirmSecret = config getString "email_confirm.secret"
  private val EmailConfirmEnabled = config getBoolean "email_confirm.enabled"
  private val PasswordResetSecret = config getString "password_reset.secret"
  private val EmailChangeSecret = config getString "email_change.secret"
  private val LoginTokenSecret = config getString "login_token.secret"
  private val TorProviderUrl = config getString "tor.provider_url"
  private val TorRefreshDelay = config duration "tor.refresh_delay"
  private val DisposableEmailProviderUrl = config getString "disposable_email.provider_url"
  private val DisposableEmailRefreshDelay = config duration "disposable_email.refresh_delay"
  private val RecaptchaPrivateKey = config getString "recaptcha.private_key"
  private val RecaptchaEndpoint = config getString "recaptcha.endpoint"
  private val NetBaseUrl = config getString "net.base_url"
  private val NetDomain = config getString "net.domain"
  private val IpIntelEmail = EmailAddress(config getString "ipintel.email")
  private val DnsApiUrl = config getString "dns_api.url"
  private val DnsApiTimeout = config duration "dns_api.timeout"

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

  lazy val ipIntel = new IpIntel(asyncCache, IpIntelEmail)

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

  lazy val automaticEmail = new AutomaticEmail(
    mailgun = mailgun,
    baseUrl = NetBaseUrl
  )

  private lazy val dnsApi = new DnsApi(DnsApiUrl, DnsApiTimeout)(system)

  lazy val emailAddressValidator = new EmailAddressValidator(disposableEmailDomain, dnsApi)

  lazy val emailBlacklistSetting = settingStore[Strings](
    "emailBlacklist",
    default = Strings(Nil),
    text = "Blacklisted email domains separated by a comma".some
  )

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    providerUrl = DisposableEmailProviderUrl,
    blacklistStr = emailBlacklistSetting.get,
    bus = system.lilaBus
  )

  import reactivemongo.bson._

  lazy val spamKeywordsSetting =
    settingStore[Strings](
      "spamKeywords",
      default = Strings(Nil),
      text = "Spam keywords separated by a comma".some
    )

  lazy val spam = new Spam(spamKeywordsSetting.get)

  scheduler.once(30 seconds)(disposableEmailDomain.refresh)
  scheduler.effect(DisposableEmailRefreshDelay, "Refresh disposable email domains")(disposableEmailDomain.refresh)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(31 seconds)(tor.refresh(_ => funit))
  scheduler.effect(TorRefreshDelay, "Refresh Tor exit nodes")(tor.refresh(firewall.unblockIps))

  lazy val ipTrust = new IpTrust(ipIntel, geoIP, tor, firewall)

  lazy val api = new SecurityApi(storeColl, firewall, geoIP, authenticator, emailAddressValidator, tryOAuthServer)(system)

  lazy val csrfRequestHandler = new CSRFRequestHandler(NetDomain)

  def cli = new Cli

  system.lilaBus.subscribeFun('fishnet) {
    case lila.hub.actorApi.fishnet.NewKey(userId, key) =>
      automaticEmail.onFishnetKey(userId, key)(lila.i18n.defaultLang)
  }

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
    captcher = lila.hub.Env.current.captcher,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
