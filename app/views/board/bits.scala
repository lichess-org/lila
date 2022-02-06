package views.html.board

import chess.format.{ FEN, Forsyth }
import controllers.routes
import play.api.libs.json.Json

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
      cls       := "mini-board mini-board--init cg-wrap is2d",
      dataState := s"${fen.value},${color.name},$lastMove"
    )(cgWrapContent)

  def miniSpan(fen: chess.format.FEN, color: chess.Color = chess.White, lastMove: String = "") =
    mini(fen, color, lastMove)(span)

  def editorJsData(fen: Option[String] = None)(implicit ctx: Context) =
    Json
      .obj(
        "baseUrl"   -> s"$netBaseUrl${routes.Editor.index}",
        "animation" -> Json.obj("duration" -> ctx.pref.animationMillis),
        "is3d"      -> ctx.pref.is3d,
        "i18n"      -> i18nJsObject(i18nKeyes)
      )
      .add("fen" -> fen)

  def explorerConfig(implicit ctx: Context) = Json.obj(
    "endpoint"          -> explorerEndpoint,
    "tablebaseEndpoint" -> tablebaseEndpoint,
    "showRatings"       -> ctx.pref.showRatings
  )

  private val i18nKeyes = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.endgamePositions,
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
