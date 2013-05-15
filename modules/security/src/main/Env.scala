package lila.security

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }
import lila.common.PimpedConfig._

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.config.Config
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    captcher: lila.hub.ActorLazyRef,
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
  }
  import settings._

  lazy val api = new Api(firewall = firewall)

  lazy val firewall = new Firewall(
    cookieName = FirewallCookieName.some filter (_ ⇒ FirewallCookieEnabled),
    enabled = FirewallEnabled,
    cachedIpsTtl = FirewallCachedIpsTtl)

  lazy val flood = new Flood(FloodDuration)

  lazy val wiretap = new Wiretap(WiretapIps)

  lazy val forms = new DataForm(captcher = captcher)

  lazy val userSpy = Store userSpy _

  lazy val deleteUser = Store deleteUser _

  {
    import scala.concurrent.duration._

    scheduler.effect(10 minutes, "firewall: refresh") {
      firewall.refresh
    }
  }

  def cli = new Cli

  private[security] lazy val storeColl = db(CollectionSecurity)
  private[security] lazy val firewallColl = db(FirewallCollectionFirewall)
}

object Env {

  lazy val current = "[boot] security" describes new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    captcher = lila.hub.Env.current.actor.captcher)
}
