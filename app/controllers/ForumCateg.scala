package controllers

import lila.app._
import views._

object ForumCateg extends LilaController with ForumController {

  def index = Open { implicit ctx =>
    ctx.userId.??(teamCache.teamIds.apply) flatMap { teamIds =>
      categApi.list(teamIds, ctx.troll) map { html.forum.categ.index(_) }
    }
  }

  def show(slug: String, page: Int) = Open { implicit ctx =>
    CategGrantRead(slug) {
      OptionOk(categApi.show(slug, page, ctx.troll)) {
        case (categ, topics) => html.forum.categ.show(categ, topics)
      }
    }
  }
}
