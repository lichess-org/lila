package views.html.mod

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.security.FingerHash

import controllers.routes

object search {

  private val email = tag("email")
  private val mark = tag("marked")

  def apply(form: Form[_], users: List[lidraughts.user.User.WithEmails])(implicit ctx: Context) =
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
              form3.select(form("as"), lidraughts.mod.UserSearch.asChoices)
            ),
            userTable(users)
          )
        )
      }

  def print(
    fh: FingerHash,
    users: List[lidraughts.user.User.WithEmails],
    uas: List[String],
    blocked: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Fingerprint",
      moreCss = cssTag("mod.misc")
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("search"),
          div(cls := "mod-search page-menu__content box")(
            div(cls := "box__top")(
              h1("Fingerprint: ", fh.value),
              postForm(cls := "box__top__actions", action := routes.Mod.printBan(fh.value, !blocked))(
                submitButton(cls := List(
                  "button text" -> true,
                  "active" -> blocked
                ))(if (blocked) "Banned" else "Ban this print")
              )
            ),
            div(cls := "box__pad")(
              h2("User agents"),
              ul(uas map { ua => li(ua) })
            ),
            br, br,
            userTable(users)
          )
        )
      }

  private def userTable(users: List[lidraughts.user.User.WithEmails])(implicit ctx: Context) =
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
          case lidraughts.user.User.WithEmails(u, emails) => tr(
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
}
