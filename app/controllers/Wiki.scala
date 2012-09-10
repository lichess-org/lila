package controllers

import lila._
import views._

object Wiki extends LilaController {

  private def api = env.wiki.api

  val home = Open { implicit ctx ⇒
    Redirect(routes.Wiki.show("Lichess-Wiki"))
  }

  def show(slug: String) = Open { implicit ctx ⇒
    IOptionOk(api show slug) {
      case (page, pages) ⇒ html.wiki.show(page, pages)
    }
  }
}
