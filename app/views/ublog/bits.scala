package views.html.ublog

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogPost

object bits {

  def mini(post: UblogPost, makeUrl: UblogPost => Call = urlOf)(implicit ctx: Context) =
    a(cls := "ublog-post-mini", href := makeUrl(post))(
      h2(cls := "ublog-post-mini__title", post.title),
      strong(cls := "ublog-post-mini__intro", post.intro)
    )

  def urlOf(post: UblogPost) = routes.Ublog.post(usernameOrId(post.user), post.slug, post.id.value)

  def editUrlOf(post: UblogPost) = routes.Ublog.edit(usernameOrId(post.user), post.id.value)
}
