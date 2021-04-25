package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.relay.RelayRound.Sync.UpstreamUrl.LccRegex
import lila.relay.RelayRoundForm.Data
import lila.relay.{ RelayRound, RelayTour }

object roundForm {

  import trans.broadcast._

  def create(form: Form[Data], tour: RelayTour)(implicit ctx: Context) =
    layout(newBroadcast.txt())(
      h1(a(href := views.html.relay.tour.url(tour))(tour.name), " • ", addRound()),
      inner(form, routes.RelayRound.create(tour.id.value), tour)
    )

  def edit(rt: RelayRound.WithTour, form: Form[Data])(implicit ctx: Context) =
    layout(rt.fullName)(
      h1("Edit ", a(href := tour.url(rt.tour))(rt.tour.name), " > ", a(href := rt.path)(rt.round.name)),
      inner(form, routes.RelayRound.update(rt.round.id.value), rt.tour),
      div(cls := "relay-round__actions")(
        postForm(action := routes.RelayRound.cloneRound(rt.round.id.value))(
          submitButton(
            cls := "button button-empty confirm"
          )(
            strong(cloneRound()),
            em("Create an new identical round for the same tournament. The games will not be included.")
          )
        ),
        postForm(action := routes.RelayRound.reset(rt.round.id.value))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(
            strong(resetRound()),
            em(
              "Delete all games of this round. The source will need to be active in order to re-create them."
            )
          )
        ),
        postForm(action := routes.Study.delete(rt.round.id.value))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(strong(deleteRound()), em("Definitively delete the round and its games."))
        )
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

  private def inner(form: Form[Data], url: play.api.mvc.Call, t: RelayTour)(implicit ctx: Context) =
    postForm(cls := "form3", action := url)(
      div(cls := "form-group")(bits.howToUse),
      form3.globalError(form),
      form3.group(form("name"), roundName())(form3.input(_)(autofocus)),
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
      form3.actions(
        a(href := tour.url(t))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
