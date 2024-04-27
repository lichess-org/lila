package views.coordinate

import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }

private lazy val ui = lila.coordinate.ui.CoordinateUi(helpers)

def show(scoreOption: Option[lila.coordinate.Score])(using PageContext) =
  views.base.layout(
    title = trans.coordinates.coordinateTraining.txt(),
    moreCss = frag(
      cssTag("coordinateTrainer"),
      cssTag("voice")
    ),
    pageModule = ui.pageModule(scoreOption),
    csp = defaultCsp.withPeer.withWebAssembly.some,
    openGraph = lila.web
      .OpenGraph(
        title = "Chess board coordinates trainer",
        url = s"$netBaseUrl${routes.Coordinate.home.url}",
        description =
          "Knowing the chessboard coordinates is a very important chess skill. A square name appears on the board and you must click on the correct square."
      )
      .some,
    zoomable = true,
    zenable = true,
    withHrefLangs = lila.web.LangPath(routes.Coordinate.home).some
  )(ui.preload)
