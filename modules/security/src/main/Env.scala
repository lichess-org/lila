package lila.security

import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class Env(
    config: Config,
    captcher: akka.actor.ActorSelection,
    system: ActorSystem,
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
    val GeoIPCacheSize = config getInt "geoip.cache_size"
    val PasswordResetMailgunApiUrl = config getString "password_reset.mailgun.api.url"
    val PasswordResetMailgunApiKey = config getString "password_reset.mailgun.api.key"
    val PasswordResetMailgunSender = config getString "password_reset.mailgun.sender"
    val PasswordResetMailgunBaseUrl = config getString "password_reset.mailgun.base_url"
    val PasswordResetSecret = config getString "password_reset.secret"
  }
  import settings._

  lazy val api = new Api(firewall = firewall)

  lazy val firewall = new Firewall(
    cookieName = FirewallCookieName.some filter (_ => FirewallCookieEnabled),
    enabled = FirewallEnabled,
    cachedIpsTtl = FirewallCachedIpsTtl)

  lazy val flood = new Flood(FloodDuration)

  lazy val wiretap = new Wiretap(WiretapIps)

  lazy val forms = new DataForm(captcher = captcher)

  lazy val geoIP = new GeoIP(
    file = GeoIPFile,
    cacheSize = GeoIPCacheSize)

  lazy val userSpy = UserSpy(firewall, geoIP) _

  lazy val disconnect = Store disconnect _

  lazy val passwordReset = new PasswordReset(
    apiUrl = PasswordResetMailgunApiUrl,
    apiKey = PasswordResetMailgunApiKey,
    sender = PasswordResetMailgunSender,
    baseUrl = PasswordResetMailgunBaseUrl,
    secret = PasswordResetSecret)

  def cli = new Cli

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  lazy val current = "[boot] security" describes new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    captcher = lila.hub.Env.current.actor.captcher)
}
