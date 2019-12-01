package lila.security

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import java.lang.annotation._
import scala.concurrent.duration.FiniteDuration

import lila.common.config._
import lila.common.EmailAddress

import SecurityConfig._

@Module
private class SecurityConfig(
    val collection: Collection,
    @ConfigName("flood.duration") val floodDuration: FiniteDuration,
    @ConfigName("geoip") val geoIP: GeoIP.Config,
    @ConfigName("password_reset.secret") val passwordResetSecret: Secret,
    @ConfigName("email_config") val emailConfirm: EmailConfirm,
    @ConfigName("email_change.secret") val emailChangeSecret: Secret,
    @ConfigName("login_token.secret") val loginTokenSecret: Secret,
    val tor: Tor,
    @ConfigName("disposable_email") val disposableEmail: DisposableEmail,
    @ConfigName("dns_api") val dnsApi: DnsApi,
    @ConfigName("check_mail_api") val checkMail: CheckMail,
    val recaptchaC: Recaptcha.Config,
    val mailgun: Mailgun.Config,
    val net: NetConfig,
    @ConfigName("ipintel.email") val ipIntelEmail: EmailAddress
)

private object SecurityConfig {

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

  implicit val loader = AutoConfig.loader[SecurityConfig]
}
