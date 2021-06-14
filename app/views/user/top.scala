package views.html
package user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object top {

  def apply(perfType: lila.rating.PerfType, users: List[User.LightPerf])(implicit ctx: Context) = {

    val title = s"${perfType.trans} top 200"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("slist"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"Leaderboard of ${perfType.trans}",
          url = s"$netBaseUrl${routes.User.topNb(200, perfType.key).url}",
          description = s"The 200 best chess players in ${perfType.trans}, sorted by rating"
        )
        .some
    )(
      main(cls := "page-small box")(
        h1(a(href := routes.User.list, dataIcon := ""), title),
        table(cls := "slist slist-pad")(
          tbody(
            users.zipWithIndex.map { case (u, i) =>
              tr(
                td(i + 1),
                td(lightUserLink(u.user)),
                td(u.rating),
                td(ratingProgress(u.progress))
              )
            }
          )
        )
      )
    )
  }

}
