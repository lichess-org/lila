package controllers

import lila._
import views._

object ForumPost extends LilaController {

  def topicApi = env.forum.topicApi
  def postApi = env.forum.postApi
  def forms = forum.DataForm

  def create(categSlug: String, slug: String, page: Int) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    IOptionResult(topicApi.show(categSlug, slug, page)) {
      case (categ, topic, posts) ⇒ forms.post.bindFromRequest.fold(
        err ⇒ BadRequest(html.forum.topic.show(categ, topic, posts, err.some)),
        data ⇒ (for {
          post ← postApi.makePost(categ, topic, data, ctx.me)
        } yield Redirect("%s#%d".format(
          routes.ForumTopic.show(categ.slug, topic.slug, postApi pageOf post),
          post.number)
        )).unsafePerformIO
      )
    }
  }

  def delete(id: String) = TODO
}
