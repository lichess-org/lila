package controllers

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import scala.concurrent.duration._
import views._

final class ForumPost(env: Env) extends LilaController(env) with ForumController {

  private val CreateRateLimit =
    new lila.memo.RateLimit[IpAddress](4, 5.minutes, key = "forum.post")

  def search(text: String, page: Int) =
    OpenBody { implicit ctx =>
      NotForKids {
        if (text.trim.isEmpty) Redirect(routes.ForumCateg.index()).fuccess
        else env.forumSearch(text, page, ctx.troll) map { html.forum.search(text, _) }
      }
    }

  def create(categSlug: String, slug: String, page: Int) =
    OpenBody { implicit ctx =>
      CategGrantWrite(categSlug) {
        implicit val req = ctx.body
        OptionFuResult(topicApi.show(categSlug, slug, page, ctx.me)) {
          case (categ, topic, posts) =>
            if (topic.closed) fuccess(BadRequest("This topic is closed"))
            else if (topic.isOld) fuccess(BadRequest("This topic is archived"))
            else
              forms.post
                .bindFromRequest()
                .fold(
                  err =>
                    for {
                      captcha     <- forms.anyCaptcha
                      unsub       <- ctx.userId ?? env.timeline.status(s"forum:${topic.id}")
                      canModCateg <- isGrantedMod(categ.slug)
                    } yield BadRequest(
                      html.forum.topic
                        .show(categ, topic, posts, Some(err -> captcha), unsub, canModCateg = canModCateg)
                    ),
                  data =>
                    CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
                      postApi.makePost(categ, topic, data) map { post =>
                        Redirect(routes.ForumPost.redirect(post.id))
                      }
                    }(rateLimitedFu)
                )
        }
      }
    }

  def edit(postId: String) =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      forms.postEdit
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.ForumPost.redirect(postId)).fuccess,
          data =>
            CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
              postApi.editPost(postId, data.changes, me).map { post =>
                Redirect(routes.ForumPost.redirect(post.id))
              }
            }(rateLimitedFu)
        )
    }

  def delete(categSlug: String, id: String) =
    Auth { implicit ctx => me =>
      CategGrantMod(categSlug) {
        postApi.delete(categSlug, id, me) map { Ok(_) }
      }
    }

  def react(id: String, reaction: String, v: Boolean) =
    Auth { implicit ctx => me =>
      postApi.react(id, me, reaction, v) map {
        _ ?? { post =>
          Ok(views.html.forum.post.reactions(post, true))
        }
      }
    }

  def redirect(id: String) =
    Open { implicit ctx =>
      OptionResult(postApi.urlData(id, ctx.me)) {
        case lila.forum.PostUrlData(categ, topic, page, number) =>
          Redirect(routes.ForumTopic.show(categ, topic, page).url + "#" + number)
      }
    }
}
