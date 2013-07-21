package controllers

import lila.app._
import lila.game.GameRepo
import views._

object Editor extends LilaController with BaseGame {

  private def env = Env.importer

  def index(fen: String) = Open { implicit ctx ⇒
    makeListMenu map { listMenu ⇒
      val f = java.net.URLDecoder.decode(fen, "UTF-8")
        .trim.takeWhile(' '!=).some.filter(_.nonEmpty)
      Ok(html.game.editor(listMenu, f))
    }
  }

  def game(id: String) = Open { implicit ctx ⇒
    OptionResult(GameRepo game id) { game ⇒
      Redirect(routes.Editor.index(
        get("fen") | (chess.format.Forsyth >> game.toChess)
      ))
    }
  }
}
