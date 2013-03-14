package lila.security

import lila.common.ConfigSettings
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config) {

  val CollectionSecurity = getString("collection.security")
  val WiretapIps = getString("wiretap.ips")
}
