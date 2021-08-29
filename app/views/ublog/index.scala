package views.html.ublog

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.UblogPost
import lila.user.User

object index {

  def apply(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog"
    ) {
      main(cls := "box box-pad page ublog-index")(
        h1(s"${user.username} blog"),
        div(cls := "ublog-index__posts")(
          div(cls := "infinite-scroll")(
            posts.currentPageResults map views.html.ublog.post.mini,
            pagerNext(posts, np => s"${routes.Ublog.index(user.username, np).url}")
          )
        )
      )
    }
}
