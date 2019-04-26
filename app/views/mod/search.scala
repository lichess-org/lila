package views.html.mod

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object search {

  private val email = tag("email")
  private val mark = tag("marked")

  def apply(form: Form[_], users: List[lila.user.User.WithEmails])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Search users",
      moreCss = cssTag("mod.misc")
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("search"),
          div(cls := "mod-search page-menu__content box")(
            h1("Search users"),
            st.form(cls := "search box__pad", action := routes.Mod.search, method := "GET")(
              input(name := "q", autofocus, placeholder := "Search by IP, email, or username", value := form("q").value),
              form3.select(form("as"), lila.mod.UserSearch.asChoices)
            ),
            users.nonEmpty option table(cls := "slist slist-pad")(
              thead(
                tr(
                  th("User"),
                  th("Games"),
                  th("Marks"),
                  th("IPban"),
                  th("Closed"),
                  th("Created"),
                  th("Active")
                )
              ),
              tbody(
                users.map {
                  case lila.user.User.WithEmails(u, emails) => tr(
                    td(
                      userLink(u, withBestRating = true, params = "?mod"),
                      email(emails.list.map(_.value).mkString(", "))
                    ),
                    td(u.count.game.localize),
                    td(
                      u.engine option mark("ENGINE"),
                      u.booster option mark("BOOSTER"),
                      u.troll option mark("SHADOWBAN")
                    ),
                    td(u.ipBan option mark("IPBAN")),
                    td(u.disabled option mark("CLOSED")),
                    td(momentFromNow(u.createdAt)),
                    td(u.seenAt.map(momentFromNow(_)))
                  )
                }
              )
            )
          )
        )
      }
}
