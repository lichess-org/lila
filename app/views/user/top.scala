package views.html
package user

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }

import lila.rating.PerfType

object top:

  def apply(perfType: PerfType, users: List[lila.core.user.LightPerf])(using ctx: PageContext) =

    val title = s"${perfType.trans} top 200"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("slist"),
      openGraph = lila.web
        .OpenGraph(
          title = s"Leaderboard of ${perfType.trans}",
          url = s"$netBaseUrl${routes.User.topNb(200, perfType.key).url}",
          description = s"The 200 best chess players in ${perfType.trans}, sorted by rating"
        )
        .some
    ):
      main(cls := "page-small box")(
        boxTop(h1(a(href := routes.User.list, dataIcon := Icon.LessThan), title)),
        table(cls := "slist slist-pad")(
          tbody(
            users.mapWithIndex: (u, i) =>
              tr(
                td(i + 1),
                td(lightUserLink(u.user)),
                ctx.pref.showRatings.option(
                  frag(
                    td(u.rating),
                    td(ratingProgress(u.progress))
                  )
                )
              )
          )
        )
      )
