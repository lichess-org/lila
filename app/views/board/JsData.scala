package views.html.board

import controllers.routes
import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.i18n.I18nKeys

object JsData extends lidraughts.Lidraughtsisms {

  def apply(
    sit: draughts.Situation,
    fen: String,
    animationDuration: Duration
  )(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(" ").take(4).mkString(" "),
    "baseUrl" -> s"$netBaseUrl${routes.Editor.load("")}",
    "color" -> sit.color.letter.toString,
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
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
