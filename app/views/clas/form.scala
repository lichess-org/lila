package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.ClasForm.Data

import controllers.routes

object form {

  def create(form: Form[Data])(implicit ctx: Context) =
    layout("New class")(
      h1("New class"),
      inner(form, routes.Clas.create)
    )

  // def edit(r: lila.relay.Relay, form: Form[Data])(implicit ctx: Context) =
  //   layout(r.name)(
  //     h1("Edit ", r.name),
  //     inner(form, routes.Relay.update(r.slug, r.id.value)),
  //     hr,
  //     postForm(action := routes.Relay.cloneRelay(r.slug, r.id.value))(
  //       submitButton(
  //         cls := "button button-empty confirm",
  //         title := "Create an new identical broadcast, for another round or a similar tournament"
  //       )("Clone the broadcast")
  //     ),
  //     hr,
  //     postForm(action := routes.Relay.reset(r.slug, r.id.value))(
  //       submitButton(
  //         cls := "button button-red button-empty confirm",
  //         title := "The source will need to be active in order to re-create the chapters!"
  //       )("Reset the broadcast")
  //     )
  //   )

  private def layout(title: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("clas")
    )(
      main(cls := "page-small box box-pad")(body)
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
