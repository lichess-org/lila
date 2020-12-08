package views.html.board

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def jsData(
      sit: chess.Situation,
      fen: String,
      animationDuration: Duration
  )(implicit ctx: Context) =
    Json.obj(
      "fen"     -> fen.split(" ").take(4).mkString(" "),
      "baseUrl" -> s"$netBaseUrl${routes.Editor.load("")}",
      "color"   -> sit.color.letter.toString,
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
      "pieceNotation" -> ctx.pref.pieceNotation,
      "i18n" -> i18nJsObject(i18nKeyes)
    )

  private val i18nKeyes = List(
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
    trans.toStudy
  ).map(_.key)
}
