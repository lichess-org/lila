package lila.api

import akka.actor._
import com.typesafe.config.Config

import play.api.libs.concurrent._
import play.api.Application
import play.api.Mode.Dev

import play.modules.reactivemongo._

final class ApiEnv private (application: Application, val config: Config) {

  val settings = new Settings(config)
  import settings._

  implicit val app = application

  lazy val db = new lila.db.DbEnv(ReactiveMongoPlugin.db)

  lazy val user = new lila.user.UserEnv(
    config = in("user"),
    db = db.apply)

  lazy val wiki = new lila.wiki.WikiEnv(
    config = in("wiki"),
    db = db.apply)

  val isDev = application.mode == Dev

  private def in(prefix: String) = config getConfig prefix
}

object ApiEnv {

  def apply(app: Application) = new ApiEnv(
    application = app,
    config = app.configuration.underlying
  )
}
