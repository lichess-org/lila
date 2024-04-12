package views.html.team
import controllers.team.routes.Team as teamRoutes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

object declinedRequest:

  def all(
      team: lila.team.Team,
      requests: Paginator[lila.team.RequestWithUser],
      search: Option[UserStr]
  )(using PageContext) =
    val title = s"${team.name} • ${trans.team.declinedRequests.txt()}"

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("team")),
      modules = jsModule("mod.team.admin")
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
            ),
            st.form(
              cls    := "search team-declined-request",
              method := "GET",
              action := teamRoutes.declinedRequests(team.id, 1)
            )(
              div(
                input(
                  st.name     := "search",
                  value       := search,
                  placeholder := trans.search.search.txt()
                ),
                submitButton(cls := "button", dataIcon := Icon.Search)
              )
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
                        trans.site.accept()
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
