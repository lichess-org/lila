package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayRound.Sync.UpstreamUrl.LccRegex
import lila.relay.RelayRoundForm.Data
import lila.relay.{ RelayRound, RelayTour }

object roundForm:

  import trans.broadcast.*

  def create(form: Form[Data], tour: RelayTour)(implicit ctx: Context) =
    layout(newBroadcast.txt())(
      boxTop(h1(a(href := routes.RelayTour.edit(tour.id.value))(tour.name), " • ", addRound())),
      standardFlash,
      inner(form, routes.RelayRound.create(tour.id.value), tour, create = true)
    )

  def edit(rt: RelayRound.WithTour, form: Form[Data])(implicit ctx: Context) =
    layout(rt.fullName)(
      boxTop(
        h1(
          "Edit ",
          a(href := routes.RelayTour.edit(rt.tour.id.value))(rt.tour.name),
          " > ",
          a(href := rt.path)(rt.round.name)
        )
      ),
      inner(form, routes.RelayRound.update(rt.round.id.value), rt.tour, create = false),
      div(cls := "relay-round__actions")(
        postForm(action := routes.RelayRound.reset(rt.round.id.value))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(
            strong(resetRound()),
            em(
              deleteAllGamesOfThisRound()
            )
          )
        ),
        postForm(action := routes.Study.delete(rt.round.id.value))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(strong(deleteRound()), em(definitivelyDeleteRound()))
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

  private def inner(form: Form[Data], url: play.api.mvc.Call, t: RelayTour, create: Boolean)(using
      ctx: Context
  ) =
    val isLcc = form("syncUrl").value.exists(LccRegex.matches)
    postForm(cls := "form3", action := url)(
      div(cls := "form-group")(
        bits.howToUse,
        create option p(dataIcon := "", cls := "text")(
          "The new round will have the same members and contributors as the previous one."
        )
      ),
      form3.globalError(form),
      form3.split(
        form3.group(form("name"), roundName(), half = true)(form3.input(_)(autofocus)),
        t.official option form3.group(form("caption"), "Homepage caption", half = true)(
          form3.input(_)
        )
      ),
      form3.group(
        form("syncUrl"),
        sourceUrlOrGameIds(),
        help = frag(
          sourceUrlHelp(),
          br,
          gameIdsHelp()
        ).some
      )(form3.input(_)),
      form3
        .group(form("syncUrlRound"), roundNumber(), help = frag("Only for livechesscloud source URLs").some)(
          form3.input(_, typ = "number")
        )(cls := (!isLcc).option("none")),
      form3.split(
        form3.group(
          form("startsAt"),
          startDate(),
          help = startDateHelp().some,
          half = true
        )(form3.flatpickr(_, minDate = None)),
        isGranted(_.Relay) option
          form3.group(
            form("throttle"),
            raw("Throttle in seconds"),
            help = raw("Optional, to manually throttle requests. Min 2s, max 60s.").some,
            half = true
          )(form3.input(_, typ = "number"))
      ),
      form3.actions(
        a(href := routes.RelayTour.redirectOrApiTour(t.slug, t.id.value))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
