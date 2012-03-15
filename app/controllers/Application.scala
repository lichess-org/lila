package controllers

import lila.http._

import play.api._
import play.api.mvc._

object Application extends Controller {

  val env = new HttpEnv(Play.unsafeApplication.configuration.underlying)

  def index = TODO
}
