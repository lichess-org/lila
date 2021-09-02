package controllers

import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.ublog.UblogPost
import lila.user.{ User => UserModel }

final class Ublog(env: Env) extends LilaController(env) {

  import views.html.ublog.post.{ editUrlOf, urlOf }
  import lila.common.paginator.Paginator.zero

  def index(username: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(env.user.repo named username) { user =>
      (canViewBlogOf(user) ?? env.ublog.api.liveByUser(user, page)) map { posts =>
        html.ublog.index(user, posts)
      }
    }
  }

  def drafts(username: String, page: Int) = Auth { implicit ctx => me =>
    if (!me.is(username)) Redirect(routes.Ublog.drafts(me.username)).fuccess
    else
      env.ublog.api.draftByUser(me, page) map { posts =>
        Ok(html.ublog.index.drafts(me, posts))
      }
  }

  def post(username: String, slug: String, id: String) = Open { implicit ctx =>
    OptionFuResult(env.user.repo named username) { user =>
      (env.ublog.api.findByAuthor(UblogPost.Id(id), user)) flatMap {
        _.filter(canViewPost(user)).fold(notFound) { post =>
          if (slug != post.slug) Redirect(urlOf(post)).fuccess
          else {
            val markup = scalatags.Text.all.raw(env.ublog.markup(post))
            Ok(html.ublog.post(user, post, markup)).fuccess
          }
        }
      }
    }
  }

  def form(username: String) = Auth { implicit ctx => me =>
    if (!me.is(username)) Redirect(routes.Ublog.form(me.username)).fuccess
    else Ok(html.ublog.form.create(me, env.ublog.form.create)).fuccess
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[UserModel.ID](
    credits = 10 * 3,
    duration = 24.hour,
    key = "ublog.create.user"
  )

  def create(unusedUsername: String) = AuthBody { implicit ctx => me =>
    env.ublog.form.create
      .bindFromRequest()(ctx.body, formBinding)
      .fold(
        err => BadRequest(html.ublog.form.create(me, err)).fuccess,
        data =>
          CreateLimitPerUser(me.id, cost = if (me.isVerified) 1 else 3) {
            env.ublog.api.create(data, me) map { post =>
              Redirect(editUrlOf(post)).flashSuccess
            }
          }(rateLimitedFu)
      )
  }

  def edit(username: String, id: String) = AuthBody { implicit ctx => me =>
    OptionOk(env.ublog.api.find(UblogPost.Id(id)).map(_.filter(_.isBy(me)))) { post =>
      html.ublog.form.edit(me, post, env.ublog.form.edit(post))
    }
  }

  def update(unusedUsername: String, id: String) = AuthBody { implicit ctx => me =>
    env.ublog.api.findByAuthor(UblogPost.Id(id), me) flatMap {
      _ ?? { prev =>
        env.ublog.form
          .edit(prev)
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err => BadRequest(html.ublog.form.edit(me, prev, err)).fuccess,
            data =>
              env.ublog.api.update(data, prev, me) map { post =>
                Redirect(urlOf(post)).flashSuccess
              }
          )
      }
    }
  }

  def delete(unusedUsername: String, id: String) = AuthBody { implicit ctx => me =>
    env.ublog.api.findByAuthor(UblogPost.Id(id), me) flatMap {
      _ ?? { post =>
        env.ublog.api.delete(post) inject Redirect(routes.Ublog.index(me.username)).flashSuccess
      }
    }
  }

  private val ImageRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "ublog.image.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  def image(id: String) =
    AuthBody(parse.multipartFormData) { implicit ctx => me =>
      env.ublog.api.findByAuthor(UblogPost.Id(id), me) flatMap {
        _ ?? { post =>
          ctx.body.body.file("image") match {
            case Some(image) =>
              ImageRateLimitPerIp(HTTPRequest ipAddress ctx.req) {
                env.ublog.api.uploadImage(post, image) map { newPost =>
                  Ok(html.ublog.form.formImage(newPost))
                } recover { case e: Exception =>
                  BadRequest(e.getMessage)
                }
              }(rateLimitedFu)
            case None => BadRequest("Missing image").fuccess
          }
        }
      }
    }

  private def canViewBlogOf(user: UserModel)(implicit ctx: Context) =
    ctx.is(user) || isGranted(_.ModerateBlog) || !user.marks.troll

  private def canViewPost(user: UserModel)(post: UblogPost)(implicit ctx: Context) =
    canViewBlogOf(user) && (ctx.is(user) || post.live)
}
