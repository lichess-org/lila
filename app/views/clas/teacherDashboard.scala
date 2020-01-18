package views.html.clas

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object teacherDashboard {

  def apply(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents students.map(_.student)))(
      cls := "clas-show teacher-dashboard",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(c.name),
        div(cls := "box__top__actions")(
          a(
            href := routes.Clas.edit(c.id.value),
            cls := "button button-empty"
          )("Edit"),
          a(
            href := routes.Clas.studentForm(c.id.value),
            cls := "button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      div(cls := "box__pad")(
        standardFlash(),
        c.desc.nonEmpty option div(cls := "clas-desc")(richText(c.desc)),
        c.archived map { archived =>
          div(cls := "clas-show__archived archived")(
            bits.showArchived(archived),
            postForm(action := routes.Clas.archive(c.id.value, false))(
              form3.submit("Restore", icon = none)(
                cls := "confirm button-empty",
                title := "Revive the class"
              )
            )
          )
        },
        clas.teachers(c)
      ),
      students.partition(_.student.isArchived) match {
        case (archived, active) =>
          frag(
            div(cls := "students")(studentList(c, active)("Students")),
            archived.nonEmpty option div(cls := "students students-archived")(
              studentList(c, archived)("Archived students")
            )
          )
      }
    )

  def studentList(c: Clas, students: List[Student.WithUser])(title: Frag)(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      table(cls := "slist slist-pad sortable")(
        thead(
          tr(
            th(attr("data-sort-default") := "1")(title),
            th("Real name"),
            sortNumberTh("Rating"),
            sortNumberTh("Games"),
            sortNumberTh("Active")
          )
        ),
        tbody(
          students.sortBy(_.user.username).map {
            case Student.WithUser(student, user) =>
              tr(
                td(
                  a(href := routes.Clas.studentShow(c.id.value, user.username))(
                    userSpan(user)
                  )
                ),
                td(student.realName),
                td(user.perfs.bestRating),
                td(user.count.game.localize),
                td(dataSort := user.seenAt.map(_.getMillis.toString))(user.seenAt.map(momentFromNowOnce))
              )
          }
        )
      )

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")
}
