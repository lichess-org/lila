package views.html.clas

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText

object studentDashboard {

  def apply(
      c: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents Nil))(
      cls := "clas-show student-dashboard",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(c.name)
      ),
      div(cls := "box__pad")(
        c.desc.nonEmpty option div(cls := "clas-desc")(richText(c.desc)),
        c.archived map { archived =>
          div(cls := "clas-show__archived archived")(bits.showArchived(archived))
        },
        clas.teachers(c)
      ),
      div(cls := "students")(studentList(students))
    )

  def studentList(students: List[Student.WithUser])(implicit ctx: Context) =
    table(cls := "slist slist-pad")(
      thead(
        tr(
          th("Students"),
          th("Rating"),
          th("Games"),
          th("Puzzles"),
          th
        )
      ),
      tbody(
        students.sortBy(-_.user.seenAt.??(_.getMillis)).map {
          case Student.WithUser(student, user) =>
            tr(
              td(
                userLink(
                  user,
                  name = span(
                    strong(user.username),
                    em(student.realName)
                  ).some
                )
              ),
              td(cls := "rating")(user.best3Perfs.map { showPerfRating(user, _) }),
              td(user.count.game.localize),
              td(user.perfs.puzzle.nb.localize),
              td(
                a(
                  dataIcon := "U",
                  cls := "button button-empty",
                  title := trans.challengeToPlay.txt(),
                  href := s"${routes.Lobby.home()}?user=${user.username}#friend"
                )
              )
            )
        }
      )
    )
}
