package lila.clas
package ui

import play.api.data.Form

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

  def teachers(clas: Clas)(using Translate) =
    div(cls := "clas-teachers")(
      trans.clas.teachersX(fragList(clas.teachers.toList.map(t => userIdLink(t.some))))
    )

  def teacherMenu(active: Either[Clas.WithStudents, String], student: Option[Student])(using Context) =
    lila.ui.bits.pageMenuSubnav(
      a(cls := active.toOption.map(_.active("classes")), href := routes.Clas.index)(
        trans.clas.lichessClasses()
      ),
      active.left.toOption.map { clas =>
        frag(
          a(cls := "active", href := routes.Clas.show(clas.clas.id.value))(clas.clas.name),
          clas.students.map { s =>
            a(
              cls  := List("student" -> true, "active" -> student.exists(s.is)),
              href := routes.Clas.studentShow(clas.clas.id.value, s.userId)
            )(
              titleNameOrId(s.userId),
              em(s.realName)
            )
          }
        )
      } | {
        a(cls := active.toOption.map(_.active("newClass")), href := routes.Clas.form)(
          trans.clas.newClass()
        )
      }
    )

  object wall:

    def show(c: Clas, html: Html)(using Context) =
      frag(
        div(cls := "clas-wall__actions")(
          a(
            dataIcon := Icon.Pencil,
            href     := routes.Clas.wallEdit(c.id.value),
            cls      := "button button-clas text"
          )(
            trans.clas.editNews()
          ),
          a(
            dataIcon := Icon.Envelope,
            href     := routes.Clas.notifyStudents(c.id.value),
            cls      := "button button-clas text"
          )(
            trans.clas.notifyAllStudents()
          )
        ),
        if c.wall.value.isEmpty then
          div(cls := "box__pad clas-wall clas-wall--empty")(trans.clas.nothingHere())
        else div(cls := "box__pad clas-wall")(rawHtml(html))
      )

    def edit(c: Clas, form: Form[?])(using Context) =
      frag(
        div(cls := "box-pad clas-wall__edit")(
          p(
            strong(trans.clas.newsEdit1()),
            ul(
              li(trans.clas.newsEdit2()),
              li(trans.clas.newsEdit3()),
              li(markdownAvailable)
            )
          ),
          postForm(cls := "form3", action := routes.Clas.wallUpdate(c.id.value))(
            form3.globalError(form),
            form3.group(
              form("wall"),
              trans.clas.classNews()
            )(form3.textarea(_)(rows := 20)),
            form3.actions(
              a(href := routes.Clas.wall(c.id.value))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          )
        )
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
