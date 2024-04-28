package views.relay

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

import lila.relay.RelayRound.Sync.UpstreamUrl.LccRegex
import lila.relay.{ RelayRound, RelayTour }

private lazy val formUi = lila.relay.ui.FormUi(helpers, bits)(tour.thumbnail.apply)

object roundForm:

  def create(form: Form[lila.relay.RelayRoundForm.Data], tour: RelayTour)(using PageContext) =
    layout(trans.broadcast.newBroadcast.txt())(
      boxTop(h1(a(href := routes.RelayTour.edit(tour.id))(tour.name), " • ", trans.broadcast.addRound())),
      standardFlash,
      formUi.round.inner(form, routes.RelayRound.create(tour.id), tour, create = true)
    )

  def edit(rt: RelayRound.WithTour, form: Form[lila.relay.RelayRoundForm.Data])(using PageContext) =
    layout(rt.fullName)(formUi.round.edit(rt, form))

  private def layout(title: String)(body: Modifier*)(using PageContext) =
    views.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      modules = EsmInit("bits.flatpickr")
    )(main(cls := "page-small box box-pad")(body))

object tourForm:

  def create(form: Form[lila.relay.RelayTourForm.Data])(using PageContext) =
    layout(trans.broadcast.newBroadcast.txt(), menu = "new".some)(
      boxTop(h1(dataIcon := Icon.RadioTower, cls := "text")(trans.broadcast.newBroadcast())),
      postForm(cls := "form3", action := routes.RelayTour.create)(
        formUi.tour.inner(form, none),
        form3.actions(
          a(href := routes.RelayTour.index(1))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      )
    )

  def edit(tg: RelayTour.WithGroupTours, form: Form[lila.relay.RelayTourForm.Data])(using PageContext) =
    layout(tg.tour.name.value, menu = none)(formUi.tour.edit(tg, form))

  private def layout(title: String, menu: Option[String])(body: Modifier*)(using PageContext) =
    views.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      modules = EsmInit("bits.relayForm")
    )(menu match
      case Some(active) =>
        main(cls := "page page-menu")(
          tour.pageMenu(active),
          div(cls := "page-menu__content box box-pad")(body)
        )
      case None => main(cls := "page-small box box-pad")(body)
    )
