package views.html
package simul

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator
import lila.user.User

object hosted:

  def apply(user: User, pager: Paginator[lila.simul.Simul])(using PageContext) =
    views.html.base.layout(
      title = s"${user.username} hosted simuls",
      moreCss = cssTag("user-simul"),
      modules = infiniteScrollTag
    ) {
      main(cls := "page-small box simul-list")(
        if pager.nbResults == 0 then
          div(cls := "box__top")(h1(userLink(user), " hasn't hosted any simuls yet!"))
        else
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(h1(userLink(user, withOnline = true), " simuls")),
                th(s"${trans.site.wins.txt()}/${trans.site.draws.txt()}/${trans.site.losses.txt()}")
              )
            ),
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map: s =>
                val hostColor = s.color match
                  case Some(color) => color
                  case None        => "random"

                tr(cls := "paginated")(
                  td(cls := "icon")(iconTag(s.mainPerfType.icon)),
                  td(cls := "name")(a(href := routes.Simul.show(s.id))(s.fullName)),
                  td(
                    span(cls := s"color-icon is $hostColor text", title := hostColor)(s.clock.config.show),
                    br,
                    momentFromNow(s.createdAt)
                  ),
                  td(s"${s.wins} / ${s.draws} / ${s.losses}")
                )
              ,
              pagerNextTable(pager, np => routes.Simul.byUser(user.username, np).url)
            )
          )
      )
    }
