package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.relay.Relay.Sync.UpstreamUrl.LccRegex
import lila.relay.RelayForm.Data
import lila.relay.{ Relay, RelayTour }

object form {

  import trans.broadcast._

  def relayCreate(form: Form[Data], tour: RelayTour)(implicit ctx: Context) =
    layout(newBroadcast.txt())(
      h1(newBroadcast()),
      inner(form, routes.Relay.create(tour.id.value))
    )

  def relayEdit(rt: Relay.WithTour, form: Form[Data])(implicit ctx: Context) =
    layout(rt.fullName)(
      h1("Edit ", rt.fullName),
      inner(form, routes.Relay.update(rt.relay.id.value)),
      hr,
      postForm(action := routes.Relay.cloneRelay(rt.relay.id.value))(
        submitButton(
          cls := "button button-empty confirm",
          title := "Create an new identical broadcast, for another round or a similar tournament"
        )(cloneBroadcast())
      ),
      hr,
      postForm(action := routes.Relay.reset(rt.relay.id.value))(
        submitButton(
          cls := "button button-red button-empty confirm",
          title := "The source will need to be active in order to re-create the chapters!"
        )(resetBroadcast())
      )
    )

  private def layout(title: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      moreJs = jsModule("flatpickr")
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
      form3.group(
        form("syncUrl"),
        sourceUrlOrGameIds(),
        help = frag(
          sourceUrlHelp(),
          br,
          gameIdsHelp()
        ).some
      )(form3.input(_)),
      form("syncUrl").value.exists(LccRegex.matches) option {
        form3.group(form("syncUrlRound"), roundNumber())(
          form3.input(_, typ = "number")(required := true)
        )
      },
      form3.split(
        form3.group(
          form("startsAt"),
          startDate(),
          help = startDateHelp().some,
          half = true
        )(form3.flatpickr(_)),
        isGranted(_.Relay) option
          form3.group(
            form("throttle"),
            raw("Throttle in seconds"),
            help = raw("Optional, to manually throttle requests. Min 2s, max 60s.").some,
            half = true
          )(form3.input(_, typ = "number"))
      ),
      isGranted(_.Relay) option form3.group(form("credit"), credits())(form3.input(_)),
      form3.actions(
        a(href := routes.Relay.index(1))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
