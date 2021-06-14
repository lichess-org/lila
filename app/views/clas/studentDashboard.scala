package views.html.clas

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText
import lila.user.User

object studentDashboard {

  def apply(
      c: Clas,
      wall: Frag,
      teachers: List[User],
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents Nil))(
      cls := "clas-show dashboard dashboard-student",
      div(cls := "clas-show__top")(
        h1(dataIcon := "", cls := "text")(c.name),
        c.desc.trim.nonEmpty option div(cls := "clas-show__desc")(richText(c.desc))
      ),
      c.archived map { archived =>
        div(cls := "box__pad")(
          div(cls := "clas-show__archived archived")(bits.showArchived(archived))
        )
      },
      table(cls := "slist slist-pad teachers")(
        thead(
          tr(
            th(trans.clas.nbTeachers(teachers.size)),
            th,
            th
          )
        ),
        tbody(
          teachers.map { user =>
            tr(
              td(
                userLink(
                  user,
                  name = span(
                    strong(user.username),
                    user.profile.flatMap(_.nonEmptyRealName) map { em(_) }
                  ).some,
                  withTitle = false
                )
              ),
              td(
                user.seenAt.map { seen =>
                  trans.lastSeenActive(momentFromNow(seen))
                }
              ),
              challengeTd(user)
            )
          }
        )
      ),
      if (c.wall.nonEmpty) div(cls := "box__pad clas-wall")(wall),
      div(cls := "students")(studentList(students))
    )

  def studentList(students: List[Student.WithUser])(implicit ctx: Context) =
    table(cls := "slist slist-pad sortable")(
      thead(
        tr(
          th(dataSortDefault)(trans.clas.nbStudents(students.size)),
          dataSortNumberTh(trans.rating()),
          dataSortNumberTh(trans.games()),
          dataSortNumberTh(trans.puzzles()),
          th
        )
      ),
      tbody(
        students.sortBy(-_.user.seenAt.??(_.getMillis)).map { case Student.WithUser(student, user) =>
          tr(
            td(
              userLink(
                user,
                name = span(
                  strong(user.username),
                  em(student.realName)
                ).some,
                withTitle = false
              )
            ),
            td(dataSort := user.perfs.bestRating, cls := "rating")(cls := "rating")(user.best3Perfs.map {
              showPerfRating(user, _)
            }),
            td(user.count.game.localize),
            td(user.perfs.puzzle.nb.localize),
            challengeTd(user)
          )
        }
      )
    )

  private def challengeTd(user: lila.user.User)(implicit ctx: Context) =
    if (ctx is user) td
    else {
      val online = isOnline(user.id)
      td(
        a(
          dataIcon := "",
          cls := List("button button-empty text" -> true, "disabled" -> !online),
          title := trans.challenge.challengeToPlay.txt(),
          href := online option s"${routes.Lobby.home}?user=${user.username}#friend"
        )(trans.play())
      )
    }
}
