package lila.api

import akka.actor._
import com.typesafe.config.Config
import play.api.Application

final class Env(
  application: Application, 
  config: Config,
  hub: lila.hub.Env,
  val isProd: Boolean) {

  val CliUsername = config getString "cli.username"

  object Net {
    val Port = config getInt "http.port"
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
  }

  lazy val cli = new Cli(hub)
}

object Env {

  lazy val current = "[boot] api" describes new Env(
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig,
    hub = lila.hub.Env.current,
    isProd = lila.common.PlayApp.isProd)
}
