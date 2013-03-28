package lila.security

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }
import lila.common.PimpedConfig._
// import site.Captcha

import com.typesafe.config.Config
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    // captcha: Captcha,
    db: lila.db.Env) {

  private val CollectionSecurity = config getString "collection.security"
  private val WiretapIps = config.getStringList("wiretap.ips").toSet
  private val FirewallEnabled = config getBoolean "firewall.enabled"
  private val FirewallCookieName = config getString "firewall.cookie.name"
  private val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
  private val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
  private val FloodDuration = config duration "flood.duration"

  lazy val api = new Api(firewall = firewall)

  lazy val storeColl = db(CollectionSecurity)

  lazy val firewall = new Firewall(
    cookieName = FirewallCookieName.some filter (_ ⇒ FirewallCookieEnabled),
    enabled = FirewallEnabled)(db(FirewallCollectionFirewall))

  lazy val flood = new Flood(FloodDuration)

  lazy val wiretap = new Wiretap(WiretapIps)

  // lazy val forms = new DataForm(
  //   captcher = captcha)

  def cli = new Cli
}

object Env {

  lazy val current = "[security] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current)
}
