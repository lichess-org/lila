package controllers

import lila.api._
import lila.user.Context

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._

object Round extends LilaController {

  def watcher(gameId: String, color: String) = TODO

  def player(fullId: String) = TODO
}
