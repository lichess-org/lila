package lila.security

import lila.common.ConfigSettings
import scala.collection.JavaConversions._
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config) {

  val CollectionSecurity = getString("collection.security")
  val WiretapIps = config.getStringList("wiretap.ips").toSet

  val FirewallEnabled = getBoolean("firewall.enabled")
  val FirewallCookieName = getString("firewall.cookie.name")
  val FirewallCookieEnabled = getBoolean("firewall.cookie.enabled")
  val FirewallCollectionFirewall = getString("firewall.collection.firewall")
}
