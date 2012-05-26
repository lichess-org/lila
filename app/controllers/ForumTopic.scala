package controllers

import lila._
import views._

object ForumTopic extends LilaController {

  def topicApi = env.forum.topicApi
  def forms = forum.DataForm

  def create(categ: String) = Open { implicit ctx =>
    BadRequest
  }

  def show(categSlug: String, slug: String, page: Int) = Open { implicit ctx ⇒
    IOptionOk(topicApi.show(categSlug, slug, page)) {
      case (categ, topic, posts) ⇒ html.forum.topic.show(categ, topic, posts, 
        postForm = (!posts.hasNextPage) option forms.post)
    }
  }

  def delete(id: String) = TODO
}
