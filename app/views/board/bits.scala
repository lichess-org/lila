package views.html.board

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def jsData(
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
    "i18n" -> i18nJsObject(translations)(ctxLang(ctx))
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
    trans.studyMenu
  )
}
