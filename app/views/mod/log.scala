package views.html.mod

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*

object log:

  def apply(logs: List[lila.mod.Modlog])(using PageContext) =

    val title = "My logs"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("log"),
        div(id := "modlog_table", cls := "page-menu__content box")(
          h1(cls := "box__top")(title),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Date"),
                th("User"),
                th("Action"),
                th("Details")
              )
            ),
            tbody(
              logs.map { log =>
                tr(
                  td(momentFromNow(log.date)),
                  td(log.user.map { u =>
                    userIdLink(u.some, params = "?mod")
                  }),
                  td(log.showAction.capitalize),
                  td(log.details)
                )
              }
            )
          )
        )
      )
    }
