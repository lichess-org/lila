package views.html.mod

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.IpAddress
import lila.security.FingerHash
import lila.mod.IpRender.RenderIp

import controllers.routes
import lila.user.User
import lila.security.Granter
import lila.user.Holder

object search {

  private val email = tag("email")
  private val mark  = tag("marked")

  def apply(mod: Holder, form: Form[_], users: List[User.WithEmails])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Search users",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          h1("Search users"),
          st.form(cls := "search box__pad", action := routes.Mod.search, method := "GET")(
            input(
              name := "q",
              autofocus,
              placeholder := "Search by IP, email, or username (exact match only)",
              value       := form("q").value
            )
          ),
          userTable(mod, users, eraseButton = isGranted(_.CloseAccount))
        )
      )
    }

  def print(
      mod: Holder,
      fh: FingerHash,
      users: List[User.WithEmails],
      uas: List[String],
      blocked: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Fingerprint",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("Fingerprint: ", fh.value),
            if (isGranted(_.Admin))
              postForm(cls := "box__top__actions", action := routes.Mod.printBan(!blocked, fh.value))(
                submitButton(
                  cls := List(
                    "button text" -> true,
                    "active"      -> blocked
                  )
                )(if (blocked) "Banned" else "Ban this print")
              )
            else if (blocked) div(cls := "banned")("BANNED")
          ),
          isGranted(_.Admin) option div(cls := "box__pad")(
            h2("User agents"),
            ul(uas map { ua =>
              li(ua)
            })
          ),
          br,
          br,
          userTable(mod, users)
        )
      )
    }

  def ip(
      mod: Holder,
      address: IpAddress,
      users: List[lila.user.User.WithEmails],
      uas: List[String],
      blocked: Boolean
  )(implicit ctx: Context, renderIp: RenderIp) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("IP address: ", renderIp(address)),
            if (isGranted(_.Admin))
              postForm(cls := "box__top__actions", action := routes.Mod.singleIpBan(!blocked, address.value))(
                submitButton(
                  cls := List(
                    "button text" -> true,
                    "active"      -> blocked
                  )
                )(if (blocked) "Banned" else "Ban this IP")
              )
            else if (blocked) div(cls := "banned")("BANNED")
          ),
          isGranted(_.Admin) option div(cls := "box__pad")(
            h2("User agents"),
            ul(uas map { ua =>
              li(ua)
            })
          ),
          br,
          br,
          userTable(mod, users)
        )
      )
    }

  def clas(mod: Holder, c: lila.clas.Clas, users: List[User.WithEmails])(implicit ctx: Context) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("Class ", a(href := routes.Clas.show(c.id.value))(c.name)),
            p("Teachers: ", c.teachers.toList.map(id => teacherLink(id)))
          ),
          br,
          br,
          userTable(mod, users)
        )
      )
    }

  def teacher(teacherId: User.ID, classes: List[lila.clas.Clas])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Classes",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          div(cls := "box__top")(
            h1("Classes from", userIdLink(teacherId.some))
          ),
          br,
          br,
          classes.nonEmpty option table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Id"),
                th("Name"),
                th("Created"),
                th("Archived"),
                th("Teachers (first is owner)")
              )
            ),
            tbody(
              classes.map(c =>
                tr(
                  td(a(href := routes.Clas.show(c.id.value))(s"${c.id}")),
                  td(c.name),
                  td(momentFromNow(c.created.at)),
                  c.archived match {
                    case None => td("No")
                    case Some(lila.clas.Clas.Recorded(closerId, at)) =>
                      td(userIdLink(closerId.some), nbsp, momentFromNow(at))
                  },
                  td(c.teachers.toList.map(id => teacherLink(id)))
                )
              )
            )
          )
        )
      )
    }

  private def teacherLink(userId: User.ID)(implicit ctx: Context) =
    lightUser(userId).map { user =>
      a(
        href     := routes.Clas.teacher(user.name),
        cls      := userClass(user.id, none, withOnline = true),
        dataHref := routes.User.show(user.name)
      )(
        lineIcon(user),
        titleTag(user),
        user.name
      )
    }

  private def userTable(mod: Holder, users: List[User.WithEmails], eraseButton: Boolean = false)(implicit
      ctx: Context
  ) =
    users.nonEmpty option table(cls := "slist slist-pad")(
      thead(
        tr(
          th("User"),
          th("Games"),
          th("Marks"),
          th("Closed"),
          th("Created"),
          th("Active"),
          isGranted(_.CloseAccount) option th,
          eraseButton option th
        )
      ),
      tbody(
        users.map { case lila.user.User.WithEmails(u, emails) =>
          tr(
            if (Granter.canViewAltUsername(mod, u))
              td(
                userLink(u, withBestRating = true, params = "?mod"),
                (isGranted(_.Admin) && isGranted(_.SetEmail)) option
                  email(emails.list.map(_.value).mkString(", "))
              )
            else td,
            td(u.count.game.localize),
            td(
              u.marks.alt option mark("ALT"),
              u.marks.engine option mark("ENGINE"),
              u.marks.boost option mark("BOOSTER"),
              u.marks.troll option mark("SHADOWBAN")
            ),
            td(u.disabled option mark("CLOSED")),
            td(momentFromNow(u.createdAt)),
            td(u.seenAt.map(momentFromNow(_))),
            isGranted(_.CloseAccount) option td(
              !u.marks.alt option button(
                cls  := "button button-empty button-thin button-red mark-alt",
                href := routes.Mod.alt(u.id, !u.marks.alt)
              )("ALT")
            ),
            eraseButton option td(
              postForm(action := routes.Mod.gdprErase(u.username))(
                submitButton(
                  cls   := "button button-red button-empty confirm",
                  title := "Definitely erase everything about this user"
                )("GDPR erasure")
              )
            )
          )
        }
      )
    )
}
