package controllers

import lila.app._
import views._

object ForumCateg extends LilaController with ForumController {

  // def index = Open { implicit ctx ⇒
  //   IOk(categApi list ~ctx.me.map(teamCache.teamIds) map {
  //     html.forum.categ.index(_)
  //   })
  // }

  def show(slug: String, page: Int) = TODO
  // Open { implicit ctx ⇒
  //   CategGrantRead(slug) {
  //     IOptionOk(categApi.show(slug, page)) {
  //       case (categ, topics) ⇒ html.forum.categ.show(categ, topics)
  //     }
  //   }
  // }
}
