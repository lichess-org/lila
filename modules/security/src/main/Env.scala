package lila.security

import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    captcher: akka.actor.ActorSelection,
    authenticator: lila.user.Authenticator,
    system: akka.actor.ActorSystem,
    scheduler: lila.common.Scheduler,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private val settings = new {
    val NetBaseUrl = config getString "net.base_url"
    val MailgunApiUrl = config getString "mailgun.api.url"
    val MailgunApiKey = config getString "mailgun.api.key"
    val MailgunSender = config getString "mailgun.sender"
    val MailgunReplyTo = config getString "mailgun.reply_to"
    val CollectionSecurity = config getString "collection.security"
    val FirewallEnabled = config getBoolean "firewall.enabled"
    val FirewallCookieName = config getString "firewall.cookie.name"
    val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
    val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
    val FirewallCachedIpsTtl = config duration "firewall.cached.ips.ttl"
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
    val RecaptchaEnabled = config getBoolean "recaptcha.enabled"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  val RecaptchaPublicKey = config getString "recaptcha.public_key"

  lazy val firewall = new Firewall(
    coll = firewallColl,
    cookieName = FirewallCookieName.some filter (_ => FirewallCookieEnabled),
    enabled = FirewallEnabled,
    asyncCache = asyncCache,
    cachedIpsTtl = FirewallCachedIpsTtl
  )

  lazy val flood = new Flood(FloodDuration)

  lazy val recaptcha: Recaptcha =
    if (RecaptchaEnabled) new RecaptchaGoogle(
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

  lazy val userSpy = UserSpy(firewall, geoIP)(storeColl) _

  def store = Store

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

  lazy val emailAddressValidator = new EmailAddressValidator(disposableEmailDomain)

  private lazy val disposableEmailDomain = new DisposableEmailDomain(
    providerUrl = DisposableEmailProviderUrl,
    busOption = system.lilaBus.some
  )

  scheduler.once(10 seconds)(disposableEmailDomain.refresh)
  scheduler.effect(DisposableEmailRefreshDelay, "Refresh disposable email domains")(disposableEmailDomain.refresh)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(30 seconds)(tor.refresh(_ => funit))
  scheduler.effect(TorRefreshDelay, "Refresh Tor exit nodes")(tor.refresh(firewall.unblockIps))

  lazy val api = new SecurityApi(storeColl, firewall, geoIP, authenticator, emailAddressValidator)

  lazy val csrfRequestHandler = new CSRFRequestHandler(NetDomain)

  def cli = new Cli

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  lazy val current = "security" boot new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    authenticator = lila.user.Env.current.authenticator,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    asyncCache = lila.memo.Env.current.asyncCache,
    captcher = lila.hub.Env.current.actor.captcher
  )
}
