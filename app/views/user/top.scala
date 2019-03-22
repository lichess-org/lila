package views.html
package user

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.rating.PerfType
import lidraughts.user.User

import controllers.routes

object top {

  def apply(perfType: lidraughts.rating.PerfType, users: List[User.LightPerf])(implicit ctx: Context) = {

    val title = s"${perfType.name} top 200"

    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag("slist"),
      responsive = true,
      openGraph = lidraughts.app.ui.OpenGraph(
        title = s"Leaderboard of ${perfType.name}",
        url = s"$netBaseUrl${routes.User.topNb(200, perfType.key).url}",
        description = s"The 200 best draughts players in ${perfType.name}, sorted by rating"
      ).some
    )(
        main(cls := "page-small box")(
          h1(a(href := routes.User.list, dataIcon := "I"), title),
          table(cls := "slist slist-pad")(
            tbody(
              users.zipWithIndex.map {
                case (u, i) => tr(
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
