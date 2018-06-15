package views.html.board

import controllers.routes
import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.I18nKeys

object JsData extends lila.Lilaisms {

  def apply(
    sit: chess.Situation,
    fen: String,
    animationDuration: Duration
  )(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(" ").take(4).mkString(" "),
    "baseUrl" -> s"$netBaseUrl${routes.Editor.load("")}",
    "color" -> sit.color.letter.toString,
    "castles" -> Json.obj(
      "K" -> (sit canCastle chess.White on chess.KingSide),
      "Q" -> (sit canCastle chess.White on chess.QueenSide),
      "k" -> (sit canCastle chess.Black on chess.KingSide),
      "q" -> (sit canCastle chess.Black on chess.QueenSide)
    ),
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
    "is3d" -> ctx.pref.is3d,
    "i18n" -> i18nJsObject(
      I18nKeys.setTheBoard,
      I18nKeys.boardEditor,
      I18nKeys.startPosition,
      I18nKeys.clearBoard,
      I18nKeys.flipBoard,
      I18nKeys.loadPosition,
      I18nKeys.popularOpenings,
      I18nKeys.castling,
      I18nKeys.whiteCastlingKingside,
      I18nKeys.blackCastlingKingside,
      I18nKeys.whitePlays,
      I18nKeys.blackPlays,
      I18nKeys.continueFromHere,
      I18nKeys.playWithTheMachine,
      I18nKeys.playWithAFriend,
      I18nKeys.analysis
    )
  )
}
