package lila.coordinate
package ui

import play.api.libs.json.Json

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class CoordinateUi(helpers: Helpers):
  import helpers.{ *, given }

  val preload = main(id := "trainer")(
    div(cls := "trainer")(
      div(cls := "side"),
      div(cls := "main-board")(chessgroundBoard),
      div(cls := "table"),
      div(cls := "progress")
    )
  )

  def pageModule(scoreOption: Option[lila.coordinate.Score])(using ctx: Context) =
    PageModule(
      "coordinateTrainer",
      Json.obj(
        "i18n"       -> i18nJsObject(i18nKeys),
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

  private val i18nKeys = List(
    trans.coordinates.aSquareIsHighlightedExplanation,
    trans.coordinates.aCoordinateAppears,
    trans.coordinates.youHaveThirtySeconds,
    trans.coordinates.goAsLongAsYouWant,
    trans.coordinates.averageScoreAsBlackX,
    trans.coordinates.averageScoreAsWhiteX,
    trans.coordinates.coordinates,
    trans.coordinates.knowingTheChessBoard,
    trans.coordinates.mostChessCourses,
    trans.coordinates.startTraining,
    trans.coordinates.talkToYourChessFriends,
    trans.coordinates.youCanAnalyseAGameMoreEffectively,
    trans.coordinates.findSquare,
    trans.coordinates.nameSquare,
    trans.coordinates.showCoordinates,
    trans.coordinates.showPieces,
    trans.storm.score,
    trans.study.back,
    trans.site.time,
    trans.site.asWhite,
    trans.site.asBlack,
    trans.site.randomColor,
    trans.site.youPlayTheWhitePieces,
    trans.site.youPlayTheBlackPieces
  )
