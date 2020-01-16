package views.html.clas

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student, Teacher }

import controllers.routes

object clas {

  def index(
      classes: List[Clas],
      teacher: Teacher.WithUser
  )(implicit ctx: Context) =
    layout("Lichess Classes", "classes")(
      cls := "clas-index",
      div(cls := "box__top")(
        h1("Lichess Classes"),
        a(
          href := routes.Clas.form,
          cls := "new button button-green button-empty",
          title := "New Class",
          dataIcon := "O"
        )
      ),
      if (classes.isEmpty)
        frag(hr, p(cls := "box__pad classes__empty")("No classes yet."))
      else
        div(cls := "classes")(
          classes.map { clas =>
            div(cls := "clas-widget", dataIcon := "f")(
              a(cls := "overlay", href := routes.Clas.show(clas.id.value)),
              div(
                h3(clas.name),
                p(clas.desc)
              )
            )
          }
        )
    )

  def show(
      clas: Clas,
      teacher: Teacher.WithUser,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    layout("Lichess Classes", "class")(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(clas.name),
        div(cls := "box__top__actions")(
          a(
            href := routes.Clas.edit(clas.id.value),
            cls := "button button-empty"
          )("Edit"),
          a(
            href := routes.Clas.form,
            cls := "new button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      div(cls := "students")(studentTable(students))
    )

  private def studentTable(students: List[Student.WithUser])(implicit ctx: Context) =
    if (students.isEmpty)
      frag(hr, p(cls := "box__pad students__empty")("No students in the class, yet."))
    else
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th("Student"),
            th("Games"),
            th("Active")
          )
        ),
        tbody(
          students.map {
            case Student.WithUser(_, user) =>
              tr(
                td(userLink(user, withBestRating = true)),
                td(user.count.game.localize),
                td(user.seenAt.map(momentFromNow(_)))
              )
          }
        )
      )

  def layout(title: String, active: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas")
    )(
      main(cls := "page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          a(cls := active.active("classes"), href := routes.Clas.index)("Classes"),
          a(cls := active.active("newClass"), href := routes.Clas.form)("New class")
        ),
        div(cls := "page-menu__content box")(body)
      )
    )
}
