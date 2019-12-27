package views.html
package user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object topActivePlayers {

  def apply(users: List[User.LightCount])(implicit ctx: Context) = {

    val title = s"Active top 200"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("slist"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"Leaderboard of top active",
          url = s"$netBaseUrl${routes.User.topNbActive(10).url}",
          description = s"The 200 best chess players in active, sorted by number of games"
        )
        .some
    )(
      main(cls := "page-small box")(
        h1(a(href := routes.User.list, dataIcon := "I"), title),
        table(cls := "slist slist-pad")(
          tbody(
            users.zipWithIndex.map {
              case (u, i) =>
                tr(
                  td(i + 1),
                  td(lightUserLink(u.user))
                )
            }
          )
        )
      )
    )
  }

}
