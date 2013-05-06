package controllers

import lila.app._
import views._

object ForumPost extends LilaController with ForumController {

  def search(text: String, page: Int) = OpenBody { implicit ctx ⇒
    text.trim.isEmpty.fold(
      Redirect(routes.ForumCateg.index).fuccess,
      Env.forumSearch(text, page, isGranted(_.StaffForum)) map { paginator ⇒
        html.forum.search(text, paginator)
      }
    )
  }

  def recent = Open { implicit ctx ⇒
    Env.forum.recent(ctx.me, teamCache.teamIds.apply) map { posts ⇒
      html.forum.post.recent(posts)
    }
  }

  def create(categSlug: String, slug: String, page: Int) = OpenBody { implicit ctx ⇒
    CategGrantWrite(categSlug) {
      implicit val req = ctx.body
      OptionFuResult(topicApi.show(categSlug, slug, page)) {
        case (categ, topic, posts) ⇒ forms.post.bindFromRequest.fold(
          err ⇒ forms.anyCaptcha map { captcha ⇒
            BadRequest(html.forum.topic.show(categ, topic, posts, Some(err -> captcha)))
          },
          data ⇒ Firewall {
            postApi.makePost(categ, topic, data) map { post ⇒
              Redirect("%s#%d".format(
                routes.ForumTopic.show(categ.slug, topic.slug, postApi lastPageOf topic.incNbPosts),
                post.number))
            }
          }
        )
      }
    }
  }

  def delete(id: String) = Secure(_.ModerateForum) { implicit ctx ⇒
    me ⇒
      postApi.delete(id, me)
  }
}
