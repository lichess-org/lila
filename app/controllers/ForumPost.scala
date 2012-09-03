package controllers

import lila._
import views._
import security.Permission

object ForumPost extends LilaController with forum.Controller {

  def topicApi = env.forum.topicApi
  def postApi = env.forum.postApi
  def forms = env.forum.forms

  val recent = Open { implicit ctx ⇒
    IOk(env.forum.recent(ctx.me) map { posts =>
      html.forum.post.recent(posts)
    })
  }

  def create(categSlug: String, slug: String, page: Int) = OpenBody { implicit ctx ⇒
    CategGrant(categSlug) {
      implicit val req = ctx.body
      IOptionResult(topicApi.show(categSlug, slug, page)) {
        case (categ, topic, posts) ⇒ forms.post.bindFromRequest.fold(
          err ⇒ BadRequest(html.forum.topic.show(
            categ, topic, posts, Some(err -> forms.captchaCreate))),
          data ⇒ UAFirewall {
            val post = postApi.makePost(categ, topic, data).unsafePerformIO
            Redirect("%s#%d".format(
            routes.ForumTopic.show(
              categ.slug,
              topic.slug,
              postApi lastPageOf topic.incNbPosts),
            post.number))
          }
        )
      }
    }
  }

  def delete(id: String) = Secure(Permission.ModerateForum) { implicit ctx ⇒
    me ⇒
      IOk(postApi.delete(id, me))
  }
}
