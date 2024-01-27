package views.html.team

import controllers.routes
import controllers.team.routes.{ Team as teamRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

object declinedRequest:

  def all(team: lila.team.Team, requests: Paginator[lila.team.RequestWithUser])(using PageContext) =
    val title = s"${team.name} • ${trans.team.declinedRequests.txt()}"

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("team"))
    ) {
      val pager = views.html.base.bits
        .paginationByQuery(teamRoutes.declinedRequests(team.id, 1), requests, showPost = true)
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          boxTop(
            h1(
              a(href := teamRoutes.show(team.id))(
                team.name
              ),
              " • ",
              trans.team.declinedRequests()
            )
          ),
          pager,
          table(cls := "slist")(
            tbody(
              requests.currentPageResults.map { request =>
                tr(
                  td(userLink(request.user)),
                  td(request.message),
                  td(momentFromNow(request.date)),
                  td(cls := "process")(
                    postForm(
                      cls    := "process-request",
                      action := teamRoutes.requestProcess(request.id)
                    )(
                      input(
                        tpe   := "hidden",
                        name  := "url",
                        value := teamRoutes.declinedRequests(team.id, requests.currentPage)
                      ),
                      button(name := "process", cls := "button button-green", value := "accept")(
                        trans.accept()
                      )
                    )
                  )
                )
              }
            )
          ),
          pager
        )
      )
    }
