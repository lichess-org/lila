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
    db: lila.db.Env,
    userRepo: UserRepo) {

  val CollectionSecurity = config getString "collection.security"
  val WiretapIps = config.getStringList("wiretap.ips").toSet
  val FirewallEnabled = config getBoolean "firewall.enabled"
  val FirewallCookieName = config getString "firewall.cookie.name"
  val FirewallCookieEnabled = config getBoolean "firewall.cookie.enabled"
  val FirewallCollectionFirewall = config getString "firewall.collection.firewall"
  val FloodDuration = config duration "flood.duration"

  lazy val api = new Api(
    store = store,
    userRepo = userRepo)

  lazy val store = new Store()(db(CollectionSecurity))

  lazy val firewall = new Firewall(
    cookieName = FirewallCookieName.some filter (_ ⇒ FirewallCookieEnabled),
    enabled = FirewallEnabled)(
      db(FirewallCollectionFirewall))

  lazy val flood = new Flood(FloodDuration)

  lazy val wiretap = new Wiretap(WiretapIps)

  // lazy val forms = new DataForm(
  //   userRepo = userRepo,
  //   captcher = captcha)

  def cli = new Cli(this, userRepo)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo)
}
