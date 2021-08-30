package controllers

import views._

import lila.api.Context
import lila.app._
import lila.ublog.UblogPost

final class Ublog(env: Env) extends LilaController(env) {

  import views.html.ublog.post.urlOf

  def index(username: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(env.user.repo named username) { user =>
      env.ublog.api.liveByUser(user, page) map { posts =>
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
      env.ublog.api.findByAuthor(UblogPost.Id(id), user) map {
        _ ?? { post =>
          if (slug != post.slug) Redirect(urlOf(post))
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
    else Ok(html.ublog.form.create(me, env.ublog.form.create)).fuccess
  }

  def create(unusedUsername: String) = AuthBody { implicit ctx => me =>
    env.ublog.form.create
      .bindFromRequest()(ctx.body, formBinding)
      .fold(
        err => BadRequest(html.ublog.form.create(me, err)).fuccess,
        data =>
          env.ublog.api.create(data, me) map { post =>
            Redirect(urlOf(post)).flashSuccess
          }
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
              env.ublog.api.update(data, prev) map { post =>
                Redirect(urlOf(post)).flashSuccess
              }
          )
      }
    }
  }
}
