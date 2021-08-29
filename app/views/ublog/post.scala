package views.html.ublog

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogPost
import lila.user.User

object post {

  def apply(user: User, post: UblogPost, markup: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • ${post.title}"
    ) {
      main(cls := "box box-pad page ublog-post")(
        h1(cls := "ublog-post__title")(post.title),
        strong(cls := "ublog-post__intro")(post.intro),
        div(cls := "ublog-post__markup")(markup)
      )
    }

  def mini(post: UblogPost)(implicit ctx: Context) =
    a(cls := "ublog-post-mini", href := url(post))(
      h2(cls := "ublog-post-mini__title", post.title),
      strong(cls := "ublog-post-mini__intro", post.intro)
    )

  def url(post: UblogPost) = routes.Ublog.post(usernameOrId(post.user), post.slug, post.id.value)
}
