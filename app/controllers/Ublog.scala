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
    NotForKids {
      OptionFuOk(env.user.repo named username) { user =>
        (canViewBlogOf(user) ?? env.ublog.paginator.byUser(user, true, page)) map { posts =>
          html.ublog.index(user, posts)
        }
      }
    }
  }

  def drafts(username: String, page: Int) = Auth { implicit ctx => me =>
    NotForKids {
      if (!me.is(username)) Redirect(routes.Ublog.drafts(me.username)).fuccess
      else
        env.ublog.paginator.byUser(me, false, page) map { posts =>
          Ok(html.ublog.index.drafts(me, posts))
        }
    }
  }

  def post(username: String, slug: String, id: String) = Open { implicit ctx =>
    NotForKids {
      OptionFuResult(env.user.repo named username) { user =>
        env.ublog.api.findByAuthor(UblogPost.Id(id), user) flatMap {
          _.filter(canViewPost(user)).fold(notFound) { post =>
            if (slug != post.slug) Redirect(urlOf(post)).fuccess
            else {
              env.ublog.api.otherPosts(user, post) zip
                ctx.me.??(env.ublog.like.liked(post)) map { case (others, liked) =>
                  lila.mon.ublog.view(user.id).increment()
                  val markup = scalatags.Text.all.raw(env.ublog.markup(post))
                  Ok(html.ublog.post(user, post, markup, others, liked))
                }
            }
          }
        }
      }
    }
  }

  def form(username: String) = Auth { implicit ctx => me =>
    NotForKids {
      if (env.ublog.api.canBlog(me)) {
        if (!me.is(username)) Redirect(routes.Ublog.form(me.username)).fuccess
        else
          env.ublog.form.anyCaptcha map { captcha =>
            Ok(html.ublog.form.create(me, env.ublog.form.create, captcha))
          }
      } else
        Unauthorized(
          html.site.message.notYet(
            "Please play a few games and wait 2 days before you can create blog posts."
          )
        ).fuccess
    }
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[UserModel.ID](
    credits = 10 * 3,
    duration = 24.hour,
    key = "ublog.create.user"
  )

  def create = AuthBody { implicit ctx => me =>
    NotForKids {
      env.ublog.form.create
        .bindFromRequest()(ctx.body, formBinding)
        .fold(
          err =>
            env.ublog.form.anyCaptcha map { captcha =>
              BadRequest(html.ublog.form.create(me, err, captcha))
            },
          data =>
            CreateLimitPerUser(me.id, cost = if (me.isVerified) 1 else 3) {
              env.ublog.api.create(data, me) map { post =>
                lila.mon.ublog.create(me.id).increment()
                Redirect(editUrlOf(post)).flashSuccess
              }
            }(rateLimitedFu)
        )
    }
  }

  def edit(id: String) = AuthBody { implicit ctx => me =>
    NotForKids {
      OptionOk(env.ublog.api.find(UblogPost.Id(id)).map(_.filter(_.isBy(me)))) { post =>
        html.ublog.form.edit(me, post, env.ublog.form.edit(post))
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    NotForKids {
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
  }

  def delete(id: String) = AuthBody { implicit ctx => me =>
    env.ublog.api.findByAuthor(UblogPost.Id(id), me) flatMap {
      _ ?? { post =>
        env.ublog.api.delete(post) inject Redirect(routes.Ublog.index(me.username)).flashSuccess
      }
    }
  }

  def like(id: String, v: Boolean) = Auth { implicit ctx => me =>
    NotForKids {
      env.ublog.like(UblogPost.Id(id), me, v) map { likes =>
        Ok(likes.value)
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
                env.ublog.api.uploadImage(me, post, image) map { newPost =>
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

  def friends(page: Int) = Open { implicit ctx =>
    NotForKids {
      Reasonable(page, 10) {
        ctx.me ?? { me =>
          env.ublog.paginator.liveByFollowed(me, page) map { posts =>
            Ok(html.ublog.index.friends(posts))
          }
        }
      }
    }
  }

  def community(page: Int) = Open { implicit ctx =>
    NotForKids {
      Reasonable(page, 10) {
        env.ublog.paginator.liveByCommunity(page) map { posts =>
          Ok(html.ublog.index.community(posts))
        }
      }
    }
  }

  private def canViewBlogOf(user: UserModel)(implicit ctx: Context) =
    ctx.is(user) || isGranted(_.ModerateBlog) || (user.enabled && !user.marks.troll)

  private def canViewPost(user: UserModel)(post: UblogPost)(implicit ctx: Context) =
    canViewBlogOf(user) && (ctx.is(user) || post.live)
}
