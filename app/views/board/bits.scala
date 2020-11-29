package views.html.board

import chess.format.{ FEN, Forsyth }
import controllers.routes
import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

object bits {

  private val dataState = attr("data-state")

  private def miniOrientation(pov: Pov): chess.Color =
    if (pov.game.variant == chess.variant.RacingKings) chess.White else pov.player.color

  def mini(pov: Pov): Tag => Tag =
    mini(
      FEN(Forsyth.boardAndColor(pov.game.situation)),
      miniOrientation(pov),
      ~pov.game.lastMoveKeys
    ) _

  def mini(fen: chess.format.FEN, color: chess.Color = chess.White, lastMove: String = "")(tag: Tag): Tag =
    tag(
      cls := "mini-board mini-board--init cg-wrap is2d",
      dataState := s"${fen.value},${color.name},$lastMove"
    )(cgWrapContent)

  def miniSpan(fen: chess.format.FEN, color: chess.Color = chess.White, lastMove: String = "") =
    mini(fen, color, lastMove)(span)

  def jsData(
      sit: chess.Situation,
      fen: FEN,
      animationDuration: Duration
  )(implicit ctx: Context) =
    Json.obj(
      "fen"     -> fen.value.split(" ").take(4).mkString(" "),
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
    trans.castling,
    trans.whiteCastlingKingside,
    trans.blackCastlingKingside,
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
