package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.mod.ModProgress._

import controllers.routes

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
                Action.all.map { a =>
                  th(a.toString)
                }
              )
            ),
            tbody(
              p.data.map { case (date, row) =>
                tr(
                  th(showDate(date)),
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
