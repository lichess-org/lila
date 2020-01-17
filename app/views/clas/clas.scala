package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.clas.ClasForm.Data
import controllers.routes

object clas {

  def index(classes: List[Clas])(implicit ctx: Context) =
    bits.layout("Lichess Classes", Right("classes"))(
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

  def showToTeacher(
      clas: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(clas.name, Left(clas))(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(clas.name),
        div(cls := "box__top__actions")(
          a(
            href := routes.Clas.edit(clas.id.value),
            cls := "button button-empty"
          )("Edit"),
          a(
            href := routes.Clas.studentForm(clas.id.value),
            cls := "button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      clas.desc.nonEmpty option div(cls := "box__pad clas-desc")(clas.desc),
      teachers(clas),
      students.partition(_.student.isArchived) match {
        case (archived, active) =>
          frag(
            div(cls := "students")(student.list(clas, active, true)("Students")),
            archived.nonEmpty option div(cls := "students students-archived")(
              student.list(clas, archived, true)("Archived students")
            )
          )
      }
    )

  def showToStudent(
      clas: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(clas.name, Left(clas))(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(clas.name)
      ),
      clas.desc.nonEmpty option div(cls := "box__pad clas-desc")(clas.desc),
      teachers(clas),
      div(cls := "students")(student.list(clas, students, false)("Students"))
    )

  private def teachers(clas: Clas) =
    p(cls := "teachers box__pad")(
      "Teachers: ",
      fragList(clas.teachers.toList.map(t => userIdLink(t.value.some)))
    )

  def create(form: Form[Data])(implicit ctx: Context) =
    bits.layout("New class", Right("newClass"))(
      cls := "box-pad",
      h1("New class"),
      innerForm(form, routes.Clas.create)
    )

  def edit(c: lila.clas.Clas, form: Form[Data])(implicit ctx: Context) =
    bits.layout(c.name, Left(c))(
      cls := "box-pad",
      h1("Edit ", c.name),
      innerForm(form, routes.Clas.update(c.id.value))
    )

  private def innerForm(form: Form[Data], url: play.api.mvc.Call)(implicit ctx: Context) =
    postForm(cls := "form3", action := url)(
      form3.globalError(form),
      form3.group(form("name"), frag("Class name"))(form3.input(_)(autofocus)),
      form3.group(form("desc"), raw("Class description"))(form3.textarea(_)(rows := 5)),
      form3.actions(
        a(href := routes.Clas.index)(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
