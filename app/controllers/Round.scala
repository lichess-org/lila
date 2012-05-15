package controllers

import lila._
import views._

import play.api.mvc._

object Round extends LilaController {

  val gameRepo = env.game.gameRepo

  def player(id: String) = Open { implicit ctx ⇒
    IOption(gameRepo pov id) { html.round.player(_) }
  }
}
