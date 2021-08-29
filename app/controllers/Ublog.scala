package controllers

import views._

import lila.api.Context
import lila.app._
import lila.ublog.UblogPost

final class Ublog(env: Env) extends LilaController(env) {

  import views.html.ublog.bits.{ url => urlOf }

  def index(username: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(env.user.repo named username) { user =>
      env.ublog.api.liveByUser(user, page) map { posts =>
        html.ublog.index(user, posts)
      }
    }
  }

  def post(username: String, slug: String, id: String) = Open { implicit ctx =>
    OptionFuResult(env.user.repo named username) { user =>
      env.ublog.api.find(UblogPost.Id(id)) map {
        _ ?? { post =>
          if (!user.is(post.user) || slug != post.slug) Redirect(urlOf(post))
          else {
            val markup = scalatags.Text.all.raw(env.ublog.markup(post.markdown))
            Ok(html.ublog.post(user, post, markup))
          }
        }
      }
    }
  }

  def form(username: String) = Auth { implicit ctx => me =>
    if (!me.is(username)) Redirect(routes.Ublog.form(me.username)).fuccess
    else Ok(html.ublog.post.create(me, env.ublog.form.create)).fuccess
  }

  def create(username: String) = AuthBody { implicit ctx => me =>
    env.ublog.form.create
      .bindFromRequest()(ctx.body, formBinding)
      .fold(
        err => BadRequest(html.ublog.post.create(me, err)).fuccess,
        data =>
          env.ublog.api.create(data, me) map { post =>
            Redirect(urlOf(post))
          }
      )
  }

  def edit(username: String, id: String) = AuthBody { implicit ctx => me =>
    OptionOk(env.ublog.api.find(UblogPost.Id(id)).map(_.filter(_.isBy(me)))) { post =>
      html.ublog.post.edit(me, post, env.ublog.form.edit(post))
    }
  }

  def update(username: String, id: String) = AuthBody { implicit ctx => me =>
    ???
  }
}
