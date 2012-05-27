package controllers

import lila._
import views._

object ForumCateg extends LilaController with forum.Controller {

  def categApi = env.forum.categApi

  val index = Open { implicit ctx ⇒
    IOk(categApi.list map { html.forum.categ.index(_) })
  }

  def show(slug: String, page: Int) = Open { implicit ctx ⇒
    CategGrant(slug) {
      IOptionOk(categApi.show(slug, page)) {
        case (categ, topics) ⇒ html.forum.categ.show(categ, topics)
      }
    }
  }
}
