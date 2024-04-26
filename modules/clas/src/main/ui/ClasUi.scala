package lila.clas
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.user.WithPerfsAndEmails

final class ClasUi(helpers: lila.ui.Helpers):
  import helpers.{ *, given }

  def showArchived(archived: Clas.Recorded)(using Translate) =
    div(
      trans.clas.removedByX(userIdLink(archived.by.some)),
      " ",
      momentFromNowOnce(archived.at)
    )

  object search:

    def clas(c: Clas, menu: Frag, userTable: Frag)(using Context) =
      main(cls := "page-menu")(
        menu,
        div(cls := "mod-search page-menu__content box")(
          boxTop(
            h1("Class ", a(href := routes.Clas.show(c.id.value))(c.name)),
            p("Teachers: ", c.teachers.toList.map(id => teacherLink(id)))
          ),
          br,
          br,
          userTable
        )
      )

    def teacher(teacherId: UserId, classes: List[Clas], menu: Frag)(using Context) =
      main(cls := "page-menu")(
        menu,
        div(cls := "mod-search page-menu__content box")(
          boxTop(
            h1("Classes from", userIdLink(teacherId.some))
          ),
          br,
          br,
          classes.nonEmpty.option(
            table(cls := "slist slist-pad")(
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
                classes.map: c =>
                  tr(
                    td(a(href := routes.Clas.show(c.id.value))(s"${c.id}")),
                    td(c.name),
                    td(momentFromNow(c.created.at)),
                    c.archived match
                      case None => td("No")
                      case Some(Clas.Recorded(closerId, at)) =>
                        td(userIdLink(closerId.some), nbsp, momentFromNow(at))
                    ,
                    td(c.teachers.toList.map(id => teacherLink(id)))
                  )
              )
            )
          )
        )
      )

    private def teacherLink(userId: UserId)(using Context) =
      lightUserSync(userId).map: user =>
        a(
          href     := routes.Clas.teacher(user.name),
          cls      := userClass(user.id, none, withOnline = true),
          dataHref := routes.User.show(user.name)
        )(
          lineIcon(user),
          titleTag(user),
          user.name
        )
