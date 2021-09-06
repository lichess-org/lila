package views.html.team

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.common.paginator.Paginator

object declinedRequest {

  def all(team: lila.team.Team, requests: Paginator[lila.team.RequestWithUser])(implicit ctx: Context) = {
    val title = s"${team.name} • ${trans.team.declinedRequests.txt()}"

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("team"))
    ) {
      val pager = views.html.base.bits
        .paginationByQuery(routes.Team.declinedRequests(team.id, 1), requests, showPost = true)
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          pager,
          table(cls := "slist")(
            tbody(
              requests.currentPageResults.map { request =>
                tr(
                  td(userLink(request.user)),
                  td(richText(request.message)),
                  td(momentFromNow(request.date)),
                  td(cls := "process")(
                    postForm(
                      cls := "process-request",
                      action := routes.Team.processDeclinedRequest(request.id)
                    )(
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
  }

}
