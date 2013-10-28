package lila.api

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    renderer: akka.actor.ActorSelection,
    bus: lila.common.Bus,
    val isProd: Boolean) {

  val CliUsername = config getString "cli.username"

  object Net {
    val Port = config getInt "http.port"
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val AssetDomain = config getString "net.asset.domain"
    val AssetVersion = config getInt "net.asset.version"
  }

  lazy val cli = new Cli(bus, renderer)
}

object Env {

  lazy val current = "[boot] api" describes new Env(
    config = lila.common.PlayApp.loadConfig,
    renderer = lila.hub.Env.current.actor.renderer,
    bus = lila.common.PlayApp.system.lilaBus,
    isProd = lila.common.PlayApp.isProd)
}
