package views.html.simul

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object home {

  def apply(
    opens: List[lila.simul.Simul],
    starteds: List[lila.simul.Simul],
    finisheds: List[lila.simul.Simul]
  )(implicit ctx: Context) = bits.layout(
    title = trans.simultaneousExhibitions.txt(),
    side = div(cls := "help")(
      trans.aboutSimul(),
      " ",
      a(cls := "more")(trans.more.frag(), "..."),
      div(cls := "more none")(
        img(src := staticUrl("images/fischer-simul.jpg"), alt := "Simul IRL with Bobby Fischer")(
          em("[1964] ", trans.aboutSimulImage.frag()),
          p(trans.aboutSimulRealLife.frag()),
          p(trans.aboutSimulRules.frag()),
          p(trans.aboutSimulSettings.frag())
        )
      )
    ).some,
    openGraph = lila.app.ui.OpenGraph(
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
