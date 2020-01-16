package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student, Teacher }
import lila.clas.ClasForm.Data
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
            cls := "button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      clas.desc.nonEmpty option div(cls := "box__pad")(clas.desc),
      div(cls := "students")(student.list(students))
    )

  def create(form: Form[Data])(implicit ctx: Context) =
    clas.layout("New class", "newClass")(
      cls := "box-pad",
      h1("New class"),
      innerForm(form, routes.Clas.create)
    )

  def edit(c: lila.clas.Clas, form: Form[Data])(implicit ctx: Context) =
    clas.layout(c.name, "editClass")(
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
