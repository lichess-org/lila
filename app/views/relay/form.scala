package views.relay

import play.api.data.Form

import lila.app.UiEnv.{ *, given }

import lila.relay.RelayRound.Sync.UpstreamUrl.LccRegex
import lila.relay.{ RelayRound, RelayTour }

private lazy val formUi = lila.relay.ui.FormUi(helpers, bits)(tour.thumbnail.apply)

object roundForm:

  def create(form: Form[lila.relay.RelayRoundForm.Data], tour: RelayTour)(using Context) =
    page(trans.broadcast.newBroadcast.txt())(
      boxTop(h1(a(href := routes.RelayTour.edit(tour.id))(tour.name), " • ", trans.broadcast.addRound())),
      standardFlash,
      formUi.round.inner(form, routes.RelayRound.create(tour.id), tour, create = true)
    )

  def edit(rt: RelayRound.WithTour, form: Form[lila.relay.RelayRoundForm.Data])(using Context) =
    page(rt.fullName)(formUi.round.edit(rt, form))

  private def page(title: String)(body: Modifier*)(using Context) =
    Page(title)
      .cssTag("relay.form")
      .js(EsmInit("bits.flatpickr")):
        main(cls := "page-small box box-pad")(body)

object tourForm:

  def create(form: Form[lila.relay.RelayTourForm.Data])(using Context) =
    page(trans.broadcast.newBroadcast.txt(), menu = "new".some):
      frag(
        boxTop(h1(dataIcon := Icon.RadioTower, cls := "text")(trans.broadcast.newBroadcast())),
        postForm(cls := "form3", action := routes.RelayTour.create)(
          formUi.tour.inner(form, none),
          form3.actions(
            a(href := routes.RelayTour.index(1))(trans.site.cancel()),
            form3.submit(trans.site.apply())
          )
        )
      )

  def edit(tg: RelayTour.WithGroupTours, form: Form[lila.relay.RelayTourForm.Data])(using Context) =
    page(tg.tour.name.value, menu = none)(formUi.tour.edit(tg, form))

  private def page(title: String, menu: Option[String])(using Context) =
    Page(title)
      .cssTag("relay.form")
      .js(EsmInit("bits.relayForm"))
      .wrap: body =>
        menu match
          case Some(active) =>
            main(cls := "page page-menu")(
              tour.pageMenu(active),
              div(cls := "page-menu__content box box-pad")(body)
            )
          case None => main(cls := "page-small box box-pad")(body)
