package views.html.coordinate

import play.api.libs.json.Json

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.pref.Pref.Color
import play.api.i18n.Lang

import controllers.routes
import lila.common.LangPath

object show:

  def apply(scoreOption: Option[lila.coordinate.Score])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.coordinates.coordinateTraining.txt(),
      moreCss = cssTag("coordinateTrainer"),
      moreJs = frag(
        jsModule("coordinateTrainer"),
        embedJsUnsafeLoadThen(
          s"""LichessCoordinateTrainer(document.getElementById('trainer'), ${safeJsonValue(
              bits.coordinateConfig(scoreOption)
            )});"""
        )
      ),
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
        div(cls   := "trainer")(
          div(cls := "side"),
          div(cls := "main-board")(chessgroundBoard),
          div(cls := "table"),
          div(cls := "progress")
        )
      )
    )
