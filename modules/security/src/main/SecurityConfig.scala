package lila.security

import io.methvin.play.autoconfig._
import scala.concurrent.duration.FiniteDuration

import lila.common.config._
import lila.common.EmailAddress

private object SecurityConfig {

  case class Root(
      collection: Collection,
      floodDuration: FiniteDuration,
      geoIP: GeoIP.Config,
      @ConfigName("password_reset.secret") passwordResetSecret: Secret,
      emailConfirm: EmailConfirm,
      @ConfigName("email_change.secret") emailChangeSecret: Secret,
      @ConfigName("login_token.secret") loginTokenSecret: Secret,
      tor: Tor,
      @ConfigName("disposable_email") disposableEmail: DisposableEmail,
      @ConfigName("dns_api") dnsApi: DnsApi,
      @ConfigName("check_mail_api") checkMail: CheckMail,
      recaptcha: Recaptcha.Config,
      mailgun: Mailgun.Config,
      net: NetConfig,
      @ConfigName("ipintel.email") ipIntelEmail: EmailAddress
  )

  case class Collection(
      security: CollName,
      printBan: CollName,
      firewall: CollName
  )
  implicit val collectionLoader = AutoConfig.loader[Collection]

  case class EmailConfirm(
      enabled: Boolean,
      secret: Secret,
      cookie: String
  )
  implicit val emailConfirmLoader = AutoConfig.loader[EmailConfirm]

  case class Tor(
      @ConfigName("provider_url") providerUrl: String,
      @ConfigName("refresh_delay") refreshDelay: FiniteDuration
  )
  implicit val torLoader = AutoConfig.loader[Tor]

  case class DisposableEmail(
      @ConfigName("provider_url") providerUrl: String,
      @ConfigName("refresh_delay") refreshDelay: FiniteDuration
  )
  implicit val disposableLoader = AutoConfig.loader[DisposableEmail]

  case class DnsApi(
      url: String,
      timeout: FiniteDuration
  )
  implicit val dnsLoader = AutoConfig.loader[DnsApi]

  case class CheckMail(
      url: String,
      key: Secret
  )
  implicit val checkMailLoader = AutoConfig.loader[CheckMail]

  implicit val loader = AutoConfig.loader[Root]
}
