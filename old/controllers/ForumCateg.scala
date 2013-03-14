package controllers

import lila.app._
import views._

object ForumCateg extends LilaController with forum.Controller {

  private def categApi = env.forum.categApi
  private def teamCache = env.team.cached

  val index = Open { implicit ctx ⇒
    IOk(categApi list ~ctx.me.map(teamCache.teamIds) map {
      html.forum.categ.index(_)
    })
  }

  def show(slug: String, page: Int) = Open { implicit ctx ⇒
    CategGrantRead(slug) {
      IOptionOk(categApi.show(slug, page)) {
        case (categ, topics) ⇒ html.forum.categ.show(categ, topics)
      }
    }
  }
}
