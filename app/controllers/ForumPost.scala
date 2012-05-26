package controllers

import lila._
import views._

object ForumPost extends LilaController {

  def postApi = env.forum.postApi

  def create(categSlug: String, slug: String) = TODO

  def delete(id: String) = TODO
}
