package views.html.coordinate

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*

object bits:

  def coordinateConfig(scoreOption: Option[lila.coordinate.Score])(using ctx: PageContext) = Json.obj(
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
    trans.time,
    trans.asWhite,
    trans.asBlack,
    trans.randomColor,
    trans.youPlayTheWhitePieces,
    trans.youPlayTheBlackPieces
  )
