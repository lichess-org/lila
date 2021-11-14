package views.html.board

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  private val dataState = attr("data-state")

  def mini(fen: shogi.format.FEN, color: shogi.Color = shogi.Sente, lastMove: String = "")(tag: Tag): Tag =
    tag(
      cls := "mini-board mini-board--init cg-wrap",
      dataState := s"${fen.value},${color.name},$lastMove"
    )(cgWrapContent)

  def miniSpan(fen: shogi.format.FEN, color: shogi.Color = shogi.Sente, lastMove: String = "") =
    mini(fen, color, lastMove)(span)

  def jsData(
      sit: shogi.Situation,
      fen: String,
      animationDuration: Duration
  )(implicit ctx: Context) =
    Json.obj(
      "fen"     -> fen.split(" ").take(4).mkString(" "),
      "baseUrl" -> s"$netBaseUrl${routes.Editor.load("")}",
      "color"   -> sit.color.letter.toString,
      "animation" -> Json.obj(
        "duration" -> ctx.pref.animationFactor * animationDuration.toMillis
      ),
      "isTall"        -> ctx.pref.isTall,
      "pieceNotation" -> ctx.pref.pieceNotation,
      "i18n"          -> i18nJsObject(i18nKeyes)
    )

  private val i18nKeyes = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.fillGotesHand,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.handicaps,
    trans.whitePlays,
    trans.blackPlays,
    trans.uwatePlays,
    trans.shitatePlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis,
    trans.toStudy
  ).map(_.key)
}
