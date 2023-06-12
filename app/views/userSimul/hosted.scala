package views.html
package userSimul

import lila.api.WebContext
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User
import lila.common.paginator.Paginator

import controllers.routes

object hosted:

  def apply(user: User, pager: Paginator[lila.simul.Simul])(using WebContext) =
    views.html.base.layout(
      title = s"${user.username} hosted simuls",
      moreCss = cssTag("user-simul"),
      moreJs = infiniteScrollTag
    ) {
      if (pager.nbResults == 0) div(cls := "box-pad")(user.username, " hasn't hosted any simuls yet!")
      else
        div(cls := "simul-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 3)(h1(userLink(user, withOnline = true), " simuls")),
                th(s"${trans.wins.txt()}/${trans.draws.txt()}/${trans.losses.txt()}"),
                th(trans.side())
              )
            ),
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map { s =>
                val hostColor = s.color match
                  case Some(color) => color
                  case None        => "random"

                tr(cls := "paginated")(
                  td(cls := "icon")(iconTag(s.mainPerfType.icon)),
                  td(cls := "name")(s.fullName),
                  td(s.clock.config.toString),
                  td(momentFromNow(s.createdAt)),
                  td(s"${s.wins} / ${s.draws} / ${s.losses}"),
                  td(i(cls := s"color-icon $hostColor"))
                )
              },
              pagerNextTable(pager, np => routes.UserSimul.path(user.username, "hosted", np).url)
            )
          )
        )
    }
