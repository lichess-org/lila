package lila.api

import akka.actor._
import com.typesafe.config.Config

import play.api.libs.concurrent._
import play.api.Application
import play.api.Mode.Dev

final class Env(application: Application, val config: Config) {

  val CliUsername = config getString "cli.username"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
  }

  val RendererName = config getString "core.renderer.name"

  lazy val cli = new Cli(this)

  val isDev = application.mode == Dev
}

object Env {

  lazy val current = new Env(
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig)

}
