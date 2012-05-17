package controllers

import lila._
import views._

import play.api.mvc._

object Analyse extends LilaController {

  val gameRepo = env.game.gameRepo

  def replay(id: String, color: String) = TODO

  def stats(id: String) = TODO
}
