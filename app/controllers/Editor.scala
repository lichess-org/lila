package controllers

import lila.app._
import views._

object Editor extends LilaController with BaseGame {

  private def env = Env.importer

  def index(fen: String) = Open { implicit ctx ⇒
    makeListMenu map { listMenu ⇒
      Ok(html.game.editor(listMenu, fen.trim.some.filter(_.nonEmpty)))
    }
  }
}
