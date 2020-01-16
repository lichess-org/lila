package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.ClasForm.Data

import controllers.routes

object form {

  def create(form: Form[Data])(implicit ctx: Context) =
    clas.layout("New class", "newClass")(
      cls := "box-pad",
      h1("New class"),
      inner(form, routes.Clas.create)
    )

  def edit(c: lila.clas.Clas, form: Form[Data])(implicit ctx: Context) =
    clas.layout(c.name, "editClass")(
      cls := "box-pad",
      h1("Edit ", c.name),
      inner(form, routes.Clas.update(c.id.value))
    )

  private def inner(form: Form[Data], url: play.api.mvc.Call)(implicit ctx: Context) =
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
