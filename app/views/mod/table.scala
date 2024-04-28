package views.mod

import lila.app.templating.Environment.{ *, given }

object table:

  private val dataSort = attr("data-sort")

  def apply(users: List[User])(using PageContext) =

    val title = "All mods"

    views.base.layout(title = title, moreCss = cssTag("mod.misc")):
      main(cls := "page-menu")(
        views.mod.ui.menu("mods"),
        div(id := "mod_table", cls := "page-menu__content box")(
          h1(cls := "box__top")(title),
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
                      lila.core.perm.Permission(user).map(_.name).mkString(", ")
                    )
                  ),
                  td(dataSort := user.seenAt.map(_.toMillis.toString))(user.seenAt.map(momentFromNowOnce))
                )
              }
            )
          )
        )
      )
