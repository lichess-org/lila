package controllers

import lila._
import views._

object ForumCateg extends LilaController {

  def api = env.forum.categApi

  val index = Open { implicit ctx ⇒
    IOk(api.list map { html.forum.categ.index(_) })
  }

}
