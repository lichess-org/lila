package views.html.coordinate

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*
import lila.core.LangPath

object show:

  def apply(scoreOption: Option[lila.coordinate.Score])(using PageContext) =
    views.html.base.layout(
      title = trans.coordinates.coordinateTraining.txt(),
      moreCss = frag(
        cssTag("coordinateTrainer"),
        cssTag("voice")
      ),
      pageModule = PageModule("coordinateTrainer", bits.coordinateConfig(scoreOption)).some,
      csp = defaultCsp.withPeer.withWebAssembly.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess board coordinates trainer",
          url = s"$netBaseUrl${routes.Coordinate.home.url}",
          description =
            "Knowing the chessboard coordinates is a very important chess skill. A square name appears on the board and you must click on the correct square."
        )
        .some,
      zoomable = true,
      zenable = true,
      withHrefLangs = LangPath(routes.Coordinate.home).some
    )(
      main(id := "trainer")(
        div(cls := "trainer")(
          div(cls := "side"),
          div(cls := "main-board")(chessgroundBoard),
          div(cls := "table"),
          div(cls := "progress")
        )
      )
    )
