package controllers

import views._

import lila.api.Context
import lila.app._
import lila.ublog.UblogPost

final class Ublog(env: Env) extends LilaController(env) {

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
          if (!user.is(post.user) || slug != post.slug) Redirect(html.ublog.post.url(post))
          else {
            val markup = scalatags.Text.all.raw(env.ublog.markup(post.markdown))
            Ok(html.ublog.post(user, post, markup))
          }
        }
      }
    }
  }
}
