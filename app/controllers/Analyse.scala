package controllers

import lila._
import views._

import play.api.mvc._

object Analyse extends LilaController {

  val gameRepo = env.game.gameRepo
  val gameInfo = env.analyse.gameInfo

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    IOptionIOk(gameRepo.pov(id, color)) { pov ⇒
      gameInfo(pov.game) map { html.analyse.replay(pov, _) }
    }
  }

  def stats(id: String) = todo
}
