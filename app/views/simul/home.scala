package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object home {

  def apply(
    opens: List[lidraughts.simul.Simul],
    starteds: List[lidraughts.simul.Simul],
    finisheds: List[lidraughts.simul.Simul]
  )(implicit ctx: Context) = bits.layout(
    title = trans.simultaneousExhibitions.txt(),
    side = div(cls := "help")(
      trans.aboutSimul(),
      " ",
      a(cls := "more")(trans.more.frag(), "..."),
      div(cls := "more none")(
        img(src := staticUrl("images/sijbrands-simul.jpg"), alt := "Simul IRL with Ton Sijbrands")(
          em("[1967] ", trans.aboutSimulImage.frag()),
          p(trans.aboutSimulRealLife.frag()),
          p(trans.aboutSimulRules.frag()),
          p(trans.aboutSimulSettings.frag())
        )
      )
    ).some,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = trans.simultaneousExhibitions.txt(),
      url = s"$netBaseUrl${routes.Simul.home}",
      description = trans.aboutSimul.txt()
    ).some
  ) {
      div(id := "simul_list", dataHref := routes.Simul.homeReload())(
        homeInner(opens, starteds, finisheds)
      )
    }
}
