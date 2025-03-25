package lila.coordinate
package ui

import play.api.libs.json.Json

import lila.ui.*

import ScalatagsTemplate.*

final class CoordinateUi(helpers: Helpers):
  import helpers.{ *, given }

  def show(scoreOption: Option[Score])(using Context) =
    Page(trans.coordinates.coordinateTraining.txt())
      .css("coordinateTrainer")
      .css("voice")
      .i18n(_.coordinates, _.storm, _.study)
      .js(pageModule(scoreOption))
      .csp(_.withPeer.withWebAssembly)
      .graph(
        title = "Chess board coordinates trainer",
        url = s"$netBaseUrl${routes.Coordinate.home.url}",
        description =
          "Knowing the chessboard coordinates is a very important chess skill. A square name appears on the board and you must click on the correct square."
      )
      .hrefLangs(LangPath(routes.Coordinate.home))
      .flag(_.zoom)
      .flag(_.zen)
      .body(preload)

  private val preload = main(id := "trainer")(
    div(cls := "trainer")(
      div(cls := "side"),
      div(cls := "main-board")(chessgroundBoard),
      div(cls := "table"),
      div(cls := "progress")
    )
  )

  private def pageModule(scoreOption: Option[lila.coordinate.Score])(using ctx: Context) =
    PageModule(
      "coordinateTrainer",
      Json.obj(
        "resizePref" -> ctx.pref.resizeHandle,
        "is3d"       -> ctx.pref.is3d,
        "scores" -> Json.obj(
          "findSquare" -> Json.obj(
            "white" -> (scoreOption.so(_.white): List[Int]),
            "black" -> (scoreOption.so(_.black): List[Int])
          ),
          "nameSquare" -> Json.obj(
            "white" -> (scoreOption.so(_.whiteNameSquare): List[Int]),
            "black" -> (scoreOption.so(_.blackNameSquare): List[Int])
          )
        )
      )
    ).some
