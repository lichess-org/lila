package views.html.mod

import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.IpAddress
import lila.mod.IpRender.RenderIp
import lila.security.{ Granter, IpTrust, IsProxy, FingerHash }
import lila.user.{ Me, User }
import lila.common.paginator.Paginator
import lila.common.String.html.richText

object search:

  def apply(form: Form[?], users: List[User.WithEmails])(using PageContext, Me) =
    views.html.base.layout(
      title = "Search users",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          h1(cls := "box__top")("Search users"),
          st.form(cls := "search box__pad", action := routes.Mod.search, method := "GET")(
            input(
              name := "q",
              autofocus,
              placeholder := "Search by IP, email, or username (exact match only)",
              value       := form("q").value
            )
          ),
          userTable(users, showUsernames = true, eraseButton = isGranted(_.GdprErase))
        )
      )
    }

  def print(
      fh: FingerHash,
      users: List[User.WithEmails],
      uas: List[String],
      blocked: Boolean
  )(using PageContext, Me) =
    views.html.base.layout(
      title = "Fingerprint",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          boxTop(
            h1("Fingerprint: ", fh.value),
            if isGranted(_.Admin) then
              postForm(cls := "box__top__actions", action := routes.Mod.printBan(!blocked, fh.value))(
                submitButton(
                  cls := List(
                    "button text" -> true,
                    "active"      -> blocked
                  )
                )(if blocked then "Banned" else "Ban this print")
              )
            else if blocked then div(cls := "banned")("BANNED")
            else emptyFrag
          ),
          userAgentsBox(uas),
          userTable(users)
        )
      )

  private def userAgentsBox(uas: List[String])(using Context) =
    isGranted(_.Admin) option div(cls := "box__pad")(
      h2("User agents"),
      ul(cls := "mod-search__user-agents")(uas.map(li(_)))
    )

  def ip(
      address: IpAddress,
      users: List[lila.user.User.WithEmails],
      uas: List[String],
      data: IpTrust.IpData,
      blocked: Boolean
  )(using ctx: PageContext, renderIp: RenderIp, mod: Me) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          boxTop(
            h1("IP address: ", renderIp(address)),
            if isGranted(_.Admin) then
              postForm(cls := "box__top__actions", action := routes.Mod.singleIpBan(!blocked, address.value))(
                submitButton(
                  cls := List(
                    "button text" -> true,
                    "active"      -> blocked
                  )
                )(if blocked then "Banned" else "Ban this IP")
              )
            else if blocked then div(cls := "banned")("BANNED")
            else emptyFrag
          ),
          div(cls := "mod-search__ip-data box__pad")(
            p(
              isGranted(_.Admin) option frag("Location: ", data.location.toString, br),
              "Proxy: ",
              data.proxy.toString
            )
          ),
          userTable(users)
        )
      )

  def clas(c: lila.clas.Clas, users: List[User.WithEmails])(using PageContext, Me) =
    views.html.base.layout(
      title = "IP address",
      moreCss = cssTag("mod.misc"),
      moreJs = jsModule("mod.search")
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          boxTop(
            h1("Class ", a(href := clasRoutes.show(c.id.value))(c.name)),
            p("Teachers: ", c.teachers.toList.map(id => teacherLink(id)))
          ),
          br,
          br,
          userTable(users)
        )
      )

  def teacher(teacherId: UserId, classes: List[lila.clas.Clas])(using PageContext) =
    views.html.base.layout(
      title = "Classes",
      moreCss = cssTag("mod.misc")
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("search"),
        div(cls := "mod-search page-menu__content box")(
          boxTop(
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
                  td(a(href := clasRoutes.show(c.id.value))(s"${c.id}")),
                  td(c.name),
                  td(momentFromNow(c.created.at)),
                  c.archived match
                    case None => td("No")
                    case Some(lila.clas.Clas.Recorded(closerId, at)) =>
                      td(userIdLink(closerId.some), nbsp, momentFromNow(at))
                  ,
                  td(c.teachers.toList.map(id => teacherLink(id)))
                )
              )
            )
          )
        )
      )

  def notes(query: String, pager: Paginator[lila.user.Note])(using PageContext) =
    views.html.base.layout(
      title = "Mod notes",
      moreCss = frag(cssTag("mod.misc"), cssTag("slist")),
      moreJs = infiniteScrollTag
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("notes"),
        div(cls := "page-menu__content box")(
          boxTop(
            h1("Mod notes"),
            div(cls := "box__top__actions")(
              st.form(cls := "search", action := routes.Mod.notes())(
                input(st.name := "q", value := query, placeholder := trans.search.search.txt())
              )
            )
          ),
          br,
          br,
          table(cls := "slist slist-pad")(
            thead(
              tr(th("Moderator"), th("Player"), th("Note"), th("Date"))
            ),
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map: note =>
                tr(cls := "paginated")(
                  td(userIdLink(note.from.some)),
                  td(userIdLink(note.to.some, params = "?notes=1")),
                  td(cls := "user-note__text")(richText(note.text, expandImg = false)),
                  td(small(momentFromNowOnce(note.date)))
                ),
              pagerNextTable(pager, np => routes.Mod.notes(np, query).url)
            )
          )
        )
      )
    }

  private def teacherLink(userId: UserId)(using PageContext) =
    lightUser(userId).map: user =>
      a(
        href     := clasRoutes.teacher(user.name),
        cls      := userClass(user.id, none, withOnline = true),
        dataHref := routes.User.show(user.name)
      )(
        lineIcon(user),
        titleTag(user),
        user.name
      )
