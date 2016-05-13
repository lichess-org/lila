package views.html.board

import controllers.routes
import play.api.libs.json.{ JsArray, Json }
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._

object JsData extends lila.Steroids {

  def apply(
    sit: chess.Situation,
    fen: String,
    animationDuration: Duration)(implicit ctx: Context) = Json.obj(
    "fen" -> fen.split(" ").headOption,
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
    "i18n" -> i18nJsObject(
      trans.startPosition,
      trans.clearBoard,
      trans.flipBoard,
      trans.loadPosition,
      trans.castling,
      trans.whiteCastlingKingside,
      trans.whiteCastlingQueenside,
      trans.blackCastlingKingside,
      trans.blackCastlingQueenside,
      trans.whitePlays,
      trans.blackPlays,
      trans.continueFromHere,
      trans.playWithTheMachine,
      trans.playWithAFriend,
      trans.analysis)
  )
}
