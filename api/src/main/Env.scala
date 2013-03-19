package lila.api

import akka.actor._
import com.typesafe.config.Config

import play.api.libs.concurrent._
import play.api.Application
import play.api.Mode.Dev

final class Env(application: Application, val config: Config) {

  val settings = new Settings(config)
  import settings._

  implicit val implicitApp = application

  lazy val cli = new Cli(this)

  val isDev = application.mode == Dev
}

object Env {

  lazy val current = new Env(
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig)

}
