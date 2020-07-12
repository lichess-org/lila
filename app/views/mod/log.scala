package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object log {

  def apply(logs: List[lila.mod.Modlog])(implicit ctx: Context) = {

    val title = "Mod logs"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("log"),
        div(id := "modlog_table", cls := "page-menu__content box")(
          h1(title, " (#tavern-log in slack)"),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Mod"),
                th("Action"),
                th("Details")
              )
            ),
            tbody(
              logs.map { log =>
                tr(
                  td(userIdLink(log.mod.some), br, momentFromNow(log.date)),
                  td(
                    log.showAction.capitalize,
                    " ",
                    log.user.map { u =>
                      userIdLink(u.some, params = "?mod")
                    }
                  ),
                  td(log.details)
                )
              }
            )
          )
        )
      )
    }
  }
}
