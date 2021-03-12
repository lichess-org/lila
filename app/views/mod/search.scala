package views.html.mod

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.IpAddress
import lila.security.FingerHash
import lila.mod.IpRender.RenderIp

import controllers.routes

object search {

  private val email = tag("email")
  private val mark  = tag("marked")

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
            input(
              name := "q",
              autofocus,
              placeholder := "Search by IP, email, or username",
              value := form("q").value
            ),
            form3.select(form("as"), lila.mod.UserSearch.asChoices)
          ),
          userTable(users)
        )
      )
    }

  def print(
      fh: FingerHash,
      users: List[lila.user.User.WithEmails],
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
            postForm(cls := "box__top__actions", action := routes.Mod.printBan(!blocked, fh.value))(
              submitButton(
                cls := List(
                  "button text" -> true,
                  "active"      -> blocked
                )
              )(if (blocked) "Banned" else "Ban this print")
            )
          ),
          div(cls := "box__pad")(
            h2("User agents"),
            ul(uas map { ua =>
              li(ua)
            })
          ),
          br,
          br,
          userTable(users)
        )
      )
    }

  def ip(
      address: IpAddress,
      users: List[lila.user.User.WithEmails],
      uas: List[String],
      blocked: Boolean
  )(implicit ctx: Context, renderIp: RenderIp) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("IP address: ", renderIp(address)),
            postForm(cls := "box__top__actions", action := routes.Mod.singleIpBan(!blocked, address.value))(
              submitButton(
                cls := List(
                  "button text" -> true,
                  "active"      -> blocked
                )
              )(if (blocked) "Banned" else "Ban this IP")
            )
          ),
          isGranted(_.Admin) option div(cls := "box__pad")(
            h2("User agents"),
            ul(uas map { ua =>
              li(ua)
            })
          ),
          br,
          br,
          userTable(users)
        )
      )
    }

  def clas(
      c: lila.clas.Clas,
      users: List[lila.user.User.WithEmails]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("Class ", a(href := routes.Clas.show(c.id.value))(c.name)),
            p("Teachers: ", c.teachers.toList.map(id => userIdLink(id.some)))
          ),
          br,
          br,
          userTable(users)
        )
      )
    }

  private def userTable(users: List[lila.user.User.WithEmails])(implicit ctx: Context) =
    users.nonEmpty option table(cls := "slist slist-pad")(
      thead(
        tr(
          th("User"),
          th("Games"),
          th("Marks"),
          th("Closed"),
          th("Created"),
          th("Active")
        )
      ),
      tbody(
        users.map { case lila.user.User.WithEmails(u, emails) =>
          tr(
            td(
              userLink(u, withBestRating = true, params = "?mod"),
              (isGranted(_.Admin) && isGranted(_.SetEmail)) option
                email(emails.list.map(_.value).mkString(", "))
            ),
            td(u.count.game.localize),
            td(
              u.marks.alt option mark("ALT"),
              u.marks.engine option mark("ENGINE"),
              u.marks.boost option mark("BOOSTER"),
              u.marks.troll option mark("SHADOWBAN")
            ),
            td(u.disabled option mark("CLOSED")),
            td(momentFromNow(u.createdAt)),
            td(u.seenAt.map(momentFromNow(_)))
          )
        }
      )
    )
}
