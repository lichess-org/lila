package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.relay.RelayTourForm.Data
import lila.relay.{ RelayRound, RelayTour }

object tourForm {

  import trans.broadcast._

  def create(form: Form[Data])(implicit ctx: Context) =
    layout(newBroadcast.txt())(
      h1(newBroadcast()),
      inner(form, routes.RelayTour.create)
    )

  def edit(tour: RelayTour, form: Form[Data])(implicit ctx: Context) =
    layout(tour.name)(
      h1("Edit ", tour.name),
      inner(form, routes.RelayTour.update(tour.id.value))
    )

  private def layout(title: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form")
    )(
      main(cls := "page-small box box-pad")(body)
    )

  private def inner(form: Form[Data], url: play.api.mvc.Call)(implicit ctx: Context) =
    postForm(cls := "form3", action := url)(
      div(cls := "form-group")(
        a(dataIcon := "", cls := "text", href := routes.Page.loneBookmark("broadcasts"))(
          "How to use Lichess Broadcasts"
        )
      ),
      form3.globalError(form),
      form3.group(form("name"), eventName())(form3.input(_)(autofocus)),
      form3.group(form("description"), eventDescription())(form3.textarea(_)(rows := 2)),
      form3.group(
        form("markup"),
        fullDescription(),
        help = fullDescriptionHelp(
          a(
            href := "https://guides.github.com/features/mastering-markdown/",
            targetBlank
          )("Markdown"),
          20000.localize
        ).some
      )(form3.textarea(_)(rows := 10)),
      if (isGranted(_.Relay))
        form3.checkbox(
          form("official"),
          raw("Official Lichess broadcast"),
          help = raw("Feature on /broadcast - for admins only").some
        )
      else form3.hidden(form("official")),
      form3.actions(
        a(href := routes.RelayTour.index(1))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
