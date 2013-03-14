package lila.api

import lila.common.ConfigSettings
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config) {

  object Cli {
    val Username = getString("cli.username")
  }
}
