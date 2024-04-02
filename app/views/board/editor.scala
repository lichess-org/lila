package views.html.board

import chess.format.Fen
import controllers.routes
import play.api.libs.json.{ JsArray, Json }

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.Json.given

object editor:

  def apply(
      fen: Option[Fen.Full],
      positionsJson: JsArray,
      endgamePositionsJson: JsArray
  )(using PageContext) =
    views.html.base.layout(
      title = trans.site.boardEditor.txt(),
      pageModule = PageModule(
        "editor",
        jsData(fen) ++ Json.obj("positions" -> positionsJson, "endgamePositions" -> endgamePositionsJson)
      ).some,
      moreCss = cssTag("editor"),
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess board editor",
          url = s"$netBaseUrl${routes.Editor.index.url}",
          description = "Load opening positions or create your own chess position on a chess board editor"
        )
        .some
    ):
      main(id := "board-editor")(
        div(cls := "board-editor")(
          div(cls := "spare"),
          div(cls := "main-board")(chessgroundBoard),
          div(cls := "spare")
        )
      )

  def jsData(fen: Option[Fen.Full] = None)(using ctx: Context) =
    Json
      .obj(
        "baseUrl"   -> s"$netBaseUrl${routes.Editor.index}",
        "animation" -> Json.obj("duration" -> ctx.pref.animationMillis),
        "is3d"      -> ctx.pref.is3d,
        "i18n"      -> i18nJsObject(i18nKeys)
      )
      .add("fen" -> fen)

  private val i18nKeys = List(
    trans.site.setTheBoard,
    trans.site.boardEditor,
    trans.site.startPosition,
    trans.site.clearBoard,
    trans.site.flipBoard,
    trans.site.loadPosition,
    trans.site.popularOpenings,
    trans.site.endgamePositions,
    trans.site.castling,
    trans.site.whiteCastlingKingside,
    trans.site.blackCastlingKingside,
    trans.site.whitePlays,
    trans.site.blackPlays,
    trans.site.variant,
    trans.site.continueFromHere,
    trans.site.playWithTheMachine,
    trans.site.playWithAFriend,
    trans.site.analysis,
    trans.site.toStudy
  )
