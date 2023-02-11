package lila.security

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }

import lila.common.config.*

import SecurityConfig.*
import play.api.ConfigLoader

@Module
final private class SecurityConfig(
    val collection: Collection,
    @ConfigName("flood.duration") val floodDuration: FiniteDuration,
    @ConfigName("geoip") val geoIP: GeoIP.Config,
    @ConfigName("password_reset.secret") val passwordResetSecret: Secret,
    @ConfigName("email_confirm") val emailConfirm: EmailConfirm,
    @ConfigName("email_change.secret") val emailChangeSecret: Secret,
    @ConfigName("login_token.secret") val loginTokenSecret: Secret,
    val tor: Tor,
    @ConfigName("disposable_email") val disposableEmail: DisposableEmail,
    @ConfigName("dns_api") val dnsApi: DnsApi,
    @ConfigName("check_mail_api") val checkMail: CheckMail,
    val hcaptcha: Hcaptcha.Config,
    @ConfigName("ip2proxy") val ip2Proxy: Ip2Proxy,
    @ConfigName("lame_name_check") val lameNameCheck: LameNameCheck,
    @ConfigName("pwned.url") val pwnedUrl: String
)

private object SecurityConfig:

  case class Collection(
      security: CollName,
      @ConfigName("print_ban") printBan: CollName,
      firewall: CollName
  )
  given ConfigLoader[Collection] = AutoConfig.loader

  case class EmailConfirm(
      enabled: Boolean,
      secret: Secret,
      cookie: String
  )
  given ConfigLoader[EmailConfirm] = AutoConfig.loader

  case class Tor(
      @ConfigName("enabled") enabled: Boolean,
      @ConfigName("provider_url") providerUrl: String,
      @ConfigName("refresh_delay") refreshDelay: FiniteDuration
  )
  given ConfigLoader[Tor] = AutoConfig.loader

  case class DisposableEmail(
      @ConfigName("enabled") enabled: Boolean,
      @ConfigName("provider_url") providerUrl: String,
      @ConfigName("refresh_delay") refreshDelay: FiniteDuration
  )
  given ConfigLoader[DisposableEmail] = AutoConfig.loader

  case class DnsApi(url: String, timeout: FiniteDuration)
  given ConfigLoader[DnsApi] = AutoConfig.loader

  case class CheckMail(
      url: String,
      key: Secret
  )
  given ConfigLoader[CheckMail] = AutoConfig.loader

  case class Ip2Proxy(
      enabled: Boolean,
      url: String
  )
  given ConfigLoader[Ip2Proxy] = AutoConfig.loader

  given ConfigLoader[LameNameCheck] = boolLoader(LameNameCheck.apply)

  given ConfigLoader[SecurityConfig] = AutoConfig.loader
