package controllers

import lila._
import views._

object ForumTopic extends LilaController {

  def topicApi = env.forum.topicApi

  def create(categ: String) = TODO

  def show(categSlug: String, slug: String, page: Int) = Open { implicit ctx ⇒
    IOptionOk(topicApi.show(categSlug, slug, page)) {
      case (categ, topic, posts) ⇒ html.forum.topic.show(categ, topic, posts)
    }
  }

  def delete(id: String) = TODO
}
