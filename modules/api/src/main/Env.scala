package lila.api

import akka.actor._
import com.typesafe.config.Config
import play.api.Application

final class Env(
    application: Application,
    config: Config,
    renderer: akka.actor.ActorSelection,
    bus: akka.event.EventStream,
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
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig,
    renderer = lila.hub.Env.current.actor.renderer,
    bus = lila.common.PlayApp.system.eventStream,
    isProd = lila.common.PlayApp.isProd)
}
