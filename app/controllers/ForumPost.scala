package controllers

import lila.app._
import views._

object ForumPost extends LilaController with ForumController {

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    text.trim.isEmpty.fold(
      Redirect(routes.ForumCateg.index).fuccess,
      Env.forumSearch(text, page, isGranted(_.StaffForum), ctx.troll) map { paginator =>
        html.forum.search(text, paginator)
      }
    )
  }

  def recent = Open { implicit ctx =>
    Env.forum.recent(ctx.me, teamCache.teamIds) map { posts =>
      html.forum.post.recent(posts)
    }
  }

  def create(categSlug: String, slug: String, page: Int) = OpenBody { implicit ctx =>
    CategGrantWrite(categSlug) {
      implicit val req = ctx.body
      OptionFuResult(topicApi.show(categSlug, slug, page, ctx.troll)) {
        case (categ, topic, posts) =>
          if (topic.closed) fuccess(BadRequest("This topic is closed"))
          else forms.post.bindFromRequest.fold(
            err => forms.anyCaptcha map { captcha =>
              BadRequest(html.forum.topic.show(categ, topic, posts, Some(err -> captcha)))
            },
            data => postApi.makePost(categ, topic, data) map { post =>
              Redirect(routes.ForumPost.redirect(post.id))
            }
          )
      }
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
