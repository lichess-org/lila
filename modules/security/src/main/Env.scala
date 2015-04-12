package lila.security

import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class Env(
    config: Config,
    captcher: akka.actor.ActorSelection,
    messenger: akka.actor.ActorSelection,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionSecurity = config getString "collection.security"
    val WiretapIps = config.getStringList("wiretap.ips").toSet
    val FirewallEnabled = config getBoolean "firewall.enabled"
    val FirewallCookieName = config getString "firewall.cookie.name"
    val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
    val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
    val FirewallCachedIpsTtl = config duration "firewall.cached.ips.ttl"
    val FloodDuration = config duration "flood.duration"
    val GeoIPFile = config getString "geoip.file"
    val GeoIPCacheTtl = config duration "geoip.cache_ttl"
    val PasswordResetMailgunApiUrl = config getString "password_reset.mailgun.api.url"
    val PasswordResetMailgunApiKey = config getString "password_reset.mailgun.api.key"
    val PasswordResetMailgunSender = config getString "password_reset.mailgun.sender"
    val PasswordResetMailgunBaseUrl = config getString "password_reset.mailgun.base_url"
    val PasswordResetSecret = config getString "password_reset.secret"
    val TorProviderUrl = config getString "tor.provider_url"
    val TorRefreshDelay = config duration "tor.refresh_delay"
    val GreeterSender = config getString "greeter.sender"
  }
  import settings._

  lazy val firewall = new Firewall(
    cookieName = FirewallCookieName.some filter (_ => FirewallCookieEnabled),
    enabled = FirewallEnabled,
    cachedIpsTtl = FirewallCachedIpsTtl)

  lazy val flood = new Flood(FloodDuration)

  lazy val wiretap = new Wiretap(WiretapIps)

  lazy val forms = new DataForm(captcher = captcher)

  lazy val geoIP = new GeoIP(
    file = GeoIPFile,
    cacheTtl = GeoIPCacheTtl)

  lazy val userSpy = UserSpy(firewall, geoIP) _

  lazy val disconnect = Store disconnect _

  lazy val passwordReset = new PasswordReset(
    apiUrl = PasswordResetMailgunApiUrl,
    apiKey = PasswordResetMailgunApiKey,
    sender = PasswordResetMailgunSender,
    baseUrl = PasswordResetMailgunBaseUrl,
    secret = PasswordResetSecret)

  lazy val tor = new Tor(TorProviderUrl)
  scheduler.once(8 seconds)(tor.refresh)
  scheduler.effect(TorRefreshDelay, "Refresh TOR exit nodes")(tor.refresh)

  lazy val api = new Api(firewall, tor)

  def cli = new Cli

  lazy val greeter = new Greeter(
    sender = GreeterSender,
    messenger = messenger)

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  lazy val current = "[boot] security" describes new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    captcher = lila.hub.Env.current.actor.captcher,
  messenger = lila.hub.Env.current.actor.messenger)
}
