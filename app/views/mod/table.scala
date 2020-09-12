package views.html.mod

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object table {

  private val dataSort = attr("data-sort")

  def apply(users: List[lila.user.User])(implicit ctx: Context) = {

    val title = "All mods"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("mods"),
        div(id := "mod_table", cls := "page-menu__content box")(
          h1(title),
          st.table(cls := "slist slist-pad sortable")(
            thead(
              tr(
                th("Mod"),
                th("Permissions"),
                th("Last seen at")
              )
            ),
            tbody(
              users.map { user =>
                tr(
                  td(userLink(user)),
                  td(
                    a(href := routes.Mod.permissions(user.username))(
                      lila.security.Permission(user.roles).map(_.name) mkString ", "
                    )
                  ),
                  td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
                )
              }
            )
          )
        )
      )
    }
  }
}
