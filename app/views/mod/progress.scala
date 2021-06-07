package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.mod.ModProgress._

import controllers.routes
import lila.report.Room

object progress {

  def apply(p: Result)(implicit ctx: Context) = {
    val title = "Moderation progress"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.progress")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("progress"),
        div(cls := "page-menu__content index box mod-progress")(
          h1(title),
          table(cls := "slist slist-pad history")(
            thead(
              tr(
                th("Date"),
                Room.all.map { r =>
                  th("Report", br, r.name)
                },
                Action.all.map { a =>
                  th("Action", br, a.toString)
                }
              )
            ),
            tbody(
              p.data.map { case (date, row) =>
                tr(
                  th(showDate(date)),
                  Room.all.map { r =>
                    td(~row.reports.get(r))
                  },
                  Action.all.map { a =>
                    td(~row.actions.get(a))
                  }
                )
              }
            )
          )
        )
      )
    }
  }
}
