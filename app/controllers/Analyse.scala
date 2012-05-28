package controllers

import lila._
import views._
import analyse._

import play.api.mvc._

object Analyse extends LilaController {

  val gameRepo = env.game.gameRepo
  val gameInfo = env.analyse.gameInfo
  val pgnDump = env.analyse.pgnDump

  def replay(id: String, color: String) = Open { implicit ctx ⇒
    IOptionIOk(gameRepo.pov(id, color)) { pov ⇒
      gameInfo(pov.game) map { html.analyse.replay(pov, _) }
    }
  }

  def stats(id: String) = Open { implicit ctx ⇒
    IOptionOk(gameRepo game id) { game ⇒
      html.analyse.stats(
        game = game,
        timeChart = new TimeChart(game))
    }
  }

  def pgn(id: String) = Open { implicit ctx ⇒
    IOptionIOk(gameRepo game id) { game ⇒
      pgnDump >> game
    }
  }

}
