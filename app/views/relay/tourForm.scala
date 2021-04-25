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
      postForm(cls := "form3", action := routes.RelayTour.create)(
        inner(form),
        form3.actions(
          a(href := routes.RelayTour.index(1))(trans.cancel()),
          form3.submit(trans.apply())
        )
      )
    )

  def edit(t: RelayTour, form: Form[Data])(implicit ctx: Context) =
    layout(t.name)(
      h1("Edit ", a(href := tour.url(t))(t.name)),
      postForm(cls := "form3", action := routes.RelayTour.update(t.id.value))(
        inner(form),
        form3.actions(
          a(href := tour.url(t))(trans.cancel()),
          form3.submit(trans.apply())
        )
      )
    )

  private def layout(title: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form")
    )(
      main(cls := "page-small box box-pad")(body)
    )

  private def inner(form: Form[Data])(implicit ctx: Context) = frag(
    div(cls := "form-group")(bits.howToUse),
    form3.globalError(form),
    form3.group(form("name"), tournamentName())(form3.input(_)(autofocus)),
    form3.group(form("description"), tournamentDescription())(form3.textarea(_)(rows := 2)),
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
    else form3.hidden(form("official"))
  )
}
