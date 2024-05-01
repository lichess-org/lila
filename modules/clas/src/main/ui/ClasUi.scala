package lila.clas
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.user.WithPerfsAndEmails

final class ClasUi(helpers: lila.ui.Helpers):
  import helpers.{ *, given }

  def ClasPage(
      title: String,
      active: Either[Clas.WithStudents, String],
      student: Option[Student] = None
  )(mods: AttrPair*)(using lila.ui.Context): Page =
    Page(title)
      .cssTag("clas")
      .js(EsmInit("bits.clas"))
      .wrap: body =>
        if Granter.opt(_.Teacher) then
          main(cls := "page-menu")(
            teacherMenu(active, student),
            div(cls := "page-menu__content box")(mods, body)
          )
        else main(cls := "page-small box")(mods, body)

  def home(using Context) =
    Page(trans.clas.lichessClasses.txt())
      .cssTag("page", "clas"):
        main(cls := "page-small box box-pad page clas-home")(
          h1(cls := "box__top")(trans.clas.lichessClasses()),
          div(cls := "clas-home__doc body")(
            p(trans.clas.teachClassesOfChessStudents()),
            h2(trans.clas.features()),
            ul(
              li(trans.clas.quicklyGenerateSafeUsernames()),
              li(trans.clas.trackStudentProgress()),
              li(trans.clas.messageAllStudents()),
              li(trans.clas.freeForAllForever())
            )
          ),
          div(cls := "clas-home__onboard")(
            postForm(action := routes.Clas.becomeTeacher)(
              submitButton(cls := "button button-fat")(trans.clas.applyToBeLichessTeacher())
            )
          )
        )

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

  private def teacherMenu(active: Either[Clas.WithStudents, String], student: Option[Student])(using
      Context
  ) =
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
