package views.html.simul

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def link(simulId: lila.simul.Simul.ID): Frag =
    a(href := routes.Simul.show(simulId))("Simultaneous exhibition")

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.noSimulFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(trans.noSimulFound()),
        p(trans.noSimulExplanation()),
        p(a(href := routes.Simul.home)(trans.returnToSimulHomepage()))
      )
    }

  def homepageSpotlight(s: lila.simul.Simul)(implicit ctx: Context) =
    a(href             := routes.Simul.show(s.id), cls := "tour-spotlight little id_@s.id")(
      iconTag("f")(cls := "img"),
      span(cls := "content")(
        span(cls := "name")(s.name, " simul"),
        span(cls := "more")(
          trans.nbPlayers.plural(s.applicants.size, s.applicants.size.localize),
          " - ",
          trans.join()
        )
      )
    )

  private[simul] def setup(sim: lila.simul.Simul)(implicit lang: play.api.i18n.Lang) =
    span(cls := List("setup" -> true, "rich" -> sim.variantRich))(
      sim.clock.config.show,
      " - ",
      sim.variants.map(v => variantName(v)).mkString(", ")
    )
}
