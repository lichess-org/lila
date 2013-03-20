package controllers

import lila.wiki._
import lila.wiki.Env.{ current ⇒ wikiEnv }
import views._

object Wiki extends LilaController {

  val home = Open { implicit ctx ⇒
    fuccess(Redirect(routes.Wiki.show("Lichess-Wiki")))
  }

  def show(slug: String) = Open { implicit ctx ⇒
    Optional(wikiEnv.api show slug) {
      case (page, pages) ⇒ "todo" // html.wiki.show(page, pages)
    }
  }
}
