package lila.api

import akka.actor._
import com.typesafe.config.Config

import play.api.Application
import play.api.Mode.Dev

final class Env(application: Application, val config: Config) {

  val CliUsername = config getString "cli.username"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
  }

  lazy val cli = new Cli(this)

  val mode = application.mode
  val isDev = mode == Dev
}

object Env {

  lazy val current = "[boot] api" describes new Env(
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig)
}
