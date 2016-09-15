package controllers

import scala.concurrent.duration._
import lila.common.HTTPRequest
import lila.app._
import play.api.mvc.BodyParsers
import views._

object ForumPost extends LilaController with ForumController {

  private val CreateRateLimit = new lila.memo.RateLimit(4, 5 minutes,
    name = "forum create post",
    key = "forum.post")

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    NotForKids {
      text.trim.isEmpty.fold(
        Redirect(routes.ForumCateg.index).fuccess,
        Env.forumSearch(text, page, isGranted(_.StaffForum), ctx.troll) map { paginator =>
          html.forum.search(text, paginator)
        }
      )
    }
  }

  def recent = Open { implicit ctx =>
    NotForKids {
      Env.forum.recent(ctx.me, teamCache.teamIds) map { posts =>
        html.forum.post.recent(posts)
      }
    }
  }

  def create(categSlug: String, slug: String, page: Int) = OpenBody { implicit ctx =>
    CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
      CategGrantWrite(categSlug) {
        implicit val req = ctx.body
        OptionFuResult(topicApi.show(categSlug, slug, page, ctx.troll)) {
          case (categ, topic, posts) =>
            if (topic.closed) fuccess(BadRequest("This topic is closed"))
            else forms.post.bindFromRequest.fold(
              err => forms.anyCaptcha flatMap { captcha =>
                ctx.userId ?? Env.timeline.status(s"forum:${topic.id}") map { unsub =>
                  BadRequest(html.forum.topic.show(categ, topic, posts, Some(err -> captcha), unsub))
                }
              },
              data => postApi.makePost(categ, topic, data) map { post =>
                Redirect(routes.ForumPost.redirect(post.id))
              }
            )
        }
      }
    }
  }

  def edit(postId: String) = AuthBody(BodyParsers.parse.tolerantText) { implicit ctx =>
      me =>
        val newText = ctx.body.body

        postApi.editPost(postId, newText).map { _ =>
          Ok()
        }
  }

  def delete(categSlug: String, id: String) = Auth { implicit ctx =>
    me =>
      CategGrantMod(categSlug) {
        postApi.delete(categSlug, id, me) map { Ok(_) }
      }
  }

  def redirect(id: String) = Open { implicit ctx =>
    OptionResult(postApi.urlData(id, ctx.troll)) {
      case lila.forum.PostUrlData(categ, topic, page, number) =>
        Redirect(routes.ForumTopic.show(categ, topic, page).url + "#" + number)
    }
  }
}
