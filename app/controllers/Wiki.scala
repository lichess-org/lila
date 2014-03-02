package controllers

import lila.app._
import views._

import play.api.mvc._, Results._

object Wiki extends LilaController {

  val home = Open { implicit ctx =>
    fuccess(Redirect(routes.Wiki.show("Lichess-Wiki")))
  }

  def show(slug: String) = Open { implicit ctx =>
    OptionOk(Env.wiki.api.show(slug, lang(ctx.req).language)) {
      case (page, pages) => html.wiki.show(page, pages)
    }
  }
}
