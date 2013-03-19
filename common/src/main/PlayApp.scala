package lila.common

import play.api.Play
import com.typesafe.config.Config

object PlayApp {

  def loadConfig: Config =
    Play.maybeApplication.map(_.configuration.underlying)
      .err("Play application is not started!")

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix
}
