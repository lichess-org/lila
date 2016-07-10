package lila.security

import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._
import lila.db.dsl.Coll
import lila.user.{ User, UserRepo }

final class Env(
    config: Config,
    captcher: akka.actor.ActorSelection,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionSecurity = config getString "collection.security"
    val FirewallEnabled = config getBoolean "firewall.enabled"
    val FirewallCookieName = config getString "firewall.cookie.name"
    val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
    val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
    val FirewallCachedIpsTtl = config duration "firewall.cached.ips.ttl"
    val FloodDuration = config duration "flood.duration"
    val GeoIPFile = config getString "geoip.file"
    val GeoIPCacheTtl = config duration "geoip.cache_ttl"
    val EmailConfirmMailgunApiUrl = config getString "email_confirm.mailgun.api.url"
    val EmailConfirmMailgunApiKey = config getString "email_confirm.mailgun.api.key"
    val EmailConfirmMailgunSender = config getString "email_confirm.mailgun.sender"
    val EmailConfirmMailgunBaseUrl = config getString "email_confirm.mailgun.base_url"
    val EmailConfirmSecret = config getString "email_confirm.secret"
    val EmailConfirmEnabled = config getBoolean "email_confirm.enabled"
    val PasswordResetMailgunApiUrl = config getString "password_reset.mailgun.api.url"
    val PasswordResetMailgunApiKey = config getString "password_reset.mailgun.api.key"
    val PasswordResetMailgunSender = config getString "password_reset.mailgun.sender"
    val PasswordResetMailgunBaseUrl = config getString "password_reset.mailgun.base_url"
    val PasswordResetSecret = config getString "password_reset.secret"
    val TorProviderUrl = config getString "tor.provider_url"
    val TorRefreshDelay = config duration "tor.refresh_delay"
    val DisposableEmailProviderUrl = config getString "disposable_email.provider_url"
    val DisposableEmailRefreshDelay = config duration "disposable_email.refresh_delay"
    val RecaptchaPrivateKey = config getString "recaptcha.private_key"
    val RecaptchaEndpoint = config getString "recaptcha.endpoint"
    val RecaptchaEnabled = config getBoolean "recaptcha.enabled"
  }
  import settings._

  val RecaptchaPublicKey = config getString "recaptcha.public_key"

  lazy val firewall = new Firewall(
    coll = firewallColl,
    cookieName = FirewallCookieName.some filter (_ => FirewallCookieEnabled),
    enabled = FirewallEnabled,
    cachedIpsTtl = FirewallCachedIpsTtl)

  lazy val flood = new Flood(FloodDuration)

  lazy val recaptcha: Recaptcha =
    if (RecaptchaEnabled) new RecaptchaGoogle(
      privateKey = RecaptchaPrivateKey,
      endpoint = RecaptchaEndpoint)
    else RecaptchaSkip

  lazy val forms = new DataForm(
    captcher = captcher,
    emailAddress = emailAddress)

  lazy val geoIP = new GeoIP(
    file = GeoIPFile,
    cacheTtl = GeoIPCacheTtl)

  lazy val userSpy = UserSpy(firewall, geoIP)(storeColl) _

  def store = Store

  lazy val disconnect = store disconnect _

  lazy val emailConfirm: EmailConfirm =
    if (EmailConfirmEnabled) new EmailConfirmMailGun(
      apiUrl = EmailConfirmMailgunApiUrl,
      apiKey = EmailConfirmMailgunApiKey,
      sender = EmailConfirmMailgunSender,
      baseUrl = EmailConfirmMailgunBaseUrl,
      secret = EmailConfirmSecret)
    else EmailConfirmSkip

  lazy val passwordReset = new PasswordReset(
    apiUrl = PasswordResetMailgunApiUrl,
    apiKey = PasswordResetMailgunApiKey,
    sender = PasswordResetMailgunSender,
    baseUrl = PasswordResetMailgunBaseUrl,
    secret = PasswordResetSecret)

  lazy val emailAddress = new EmailAddress(disposableEmailDomain)

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    providerUrl = DisposableEmailProviderUrl,
    busOption = system.lilaBus.some)

  scheduler.once(10 seconds)(disposableEmailDomain.refresh)
  scheduler.effect(DisposableEmailRefreshDelay, "Refresh disposable email domains")(disposableEmailDomain.refresh)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(30 seconds)(tor.refresh(_ => funit))
  scheduler.effect(TorRefreshDelay, "Refresh Tor exit nodes")(tor.refresh(firewall.unblockIps))

  lazy val api = new Api(storeColl, firewall, geoIP, emailAddress)

  def cli = new Cli

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  lazy val current = "security" boot new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    captcher = lila.hub.Env.current.actor.captcher)
}
