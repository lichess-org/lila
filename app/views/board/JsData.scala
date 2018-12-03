package views.html.board

import controllers.routes
import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.i18n.{ I18nKeys => trans }

object JsData extends lidraughts.Lidraughtsisms {

  def apply(
    sit: draughts.Situation,
    fen: String,
    animationDuration: Duration
  )(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(":").take(3).mkString(":"),
    "coords" -> ctx.pref.coords,
    "variant" -> sit.board.variant.key,
    "baseUrl" -> s"$netBaseUrl${routes.Editor.parse("")}",
    "color" -> sit.color.letter.toString,
    "animation" -> Json.obj(
      "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
    ),
    "i18n" -> i18nJsObject(translations)
  )

  private val translations = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.whitePlays,
    trans.blackPlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis,
    trans.study
  )
}
