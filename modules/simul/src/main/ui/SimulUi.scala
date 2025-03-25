package lila.simul
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SimulUi(helpers: Helpers):
  import helpers.{ *, given }

  def link(simulId: SimulId): Tag =
    a(href := routes.Simul.show(simulId))("Simultaneous exhibition")

  def notFound(using Context) =
    Page(trans.site.noSimulFound.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.noSimulFound()),
        p(trans.site.noSimulExplanation()),
        p(a(href := routes.Simul.home)(trans.site.returnToSimulHomepage()))
      )

  def homepageSpotlight(s: Simul)(using Context) =
    a(href := routes.Simul.show(s.id), cls := "tour-spotlight little")(
      img(cls := "img icon", src := assetUrl("images/fire-silhouette.svg")),
      span(cls := "content")(
        span(cls := "name")(s.name, (!s.name.toLowerCase.endsWith(" simul")).so(" simul")),
        span(cls := "more")(
          trans.site.nbPlayers.plural(s.applicants.size, s.applicants.size.localize),
          " • ",
          trans.site.join()
        )
      )
    )

  def allCreated(simuls: Seq[Simul], withName: Boolean = true)(using Translate) =
    table(cls := "slist"):
      simuls.map: simul =>
        val url = routes.Simul.show(simul.id)
        tr(
          withName.option(td(cls := "name")(a(href := url)(simul.fullName))),
          td:
            if withName then userIdLink(simul.hostId.some)
            else a(href := url)(userIdSpanMini(simul.hostId, true))
          ,
          td(cls := "text", dataIcon := Icon.Clock)(simul.clock.config.show),
          td(cls := "text", dataIcon := Icon.User)(simul.applicants.size)
        )

  def setup(sim: Simul) =
    span(cls := List("setup" -> true, "rich" -> sim.variantRich))(
      sim.clock.config.show,
      " • ",
      sim.variants.map(_.name).mkString(", ")
    )

  def roundOtherGames(s: Simul) =
    span(cls := "simul")(
      a(href := routes.Simul.show(s.id))("SIMUL"),
      span(cls := "win")(s.wins, " W"),
      " / ",
      span(cls := "draw")(s.draws, " D"),
      " / ",
      span(cls := "loss")(s.losses, " L"),
      " / ",
      s.ongoing,
      " ongoing"
    )
